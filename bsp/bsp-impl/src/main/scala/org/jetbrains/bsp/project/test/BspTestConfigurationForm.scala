package org.jetbrains.bsp.project.test

import consulo.application.Application
import consulo.configurable.ConfigurationException
import consulo.disposer.Disposable
import consulo.execution.configuration.fragment.{RunConfigurationFragment, RunConfigurationFragmentEditor, RunConfigurationFragmentLayout}
import consulo.localize.LocalizeValue
import consulo.platform.base.icon.PlatformIconGroup
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.VerticalLayout
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{Button, ComboBox, Component, ListBox, TextBox}
import org.jetbrains.bsp.BspBundle

import java.util.regex.{Pattern, PatternSyntaxException}
import java.util as j
import scala.jdk.CollectionConverters.*

// upstream: the run configuration editor (getConfigurationEditor), Consulo edits run configurations with fragments
final class BspTestRunConfigurationFragment extends RunConfigurationFragment[BspTestRunConfiguration] {
  override def getId: String = "bsp.test"

  override def getName: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.test.kind"))

  override def isRemovable: Boolean = false

  @RequiredUIAccess
  override def createEditor(project: Project, parent: Disposable): RunConfigurationFragmentEditor[BspTestRunConfiguration] =
    new BspTestConfigurationForm(project)
}

// upstream: Java SettingsEditor with a GUI designer form (JComboBox, JTextField, JBList, JButton)
class BspTestConfigurationForm(project: Project) extends RunConfigurationFragmentEditor[BspTestRunConfiguration] {

  import BspTestConfigurationForm.TestMode

  private var testModeCombobox: ComboBox[TestMode] = scala.compiletime.uninitialized
  private var testClassFormWrapper: VerticalLayout = scala.compiletime.uninitialized
  private var testClassNameRegex: TextBox = scala.compiletime.uninitialized
  private val matchedClassesModel: MutableFlatDataModel[String] = FlatDataModel.of(j.List.of[String]())
  private var lastRequestResult: j.List[TestClass] = j.Collections.emptyList()

  @RequiredUIAccess
  override def addRows(layout: RunConfigurationFragmentLayout): Unit =
    layout.addUnlabeled(createUIComponent())

  @RequiredUIAccess
  private def createUIComponent(): Component = {
    testModeCombobox = ComboBox.create(TestMode.values*)
    testModeCombobox.setTextRenderer(mode => LocalizeValue.of(mode.toString))
    testModeCombobox.addValueListener(event => Option(event.getValue).foreach(onSelectModeChanged))

    testClassNameRegex = TextBox.create()
    testClassNameRegex.addValueListener(_ => onClassNameRegexChanged())

    // upstream: the empty text of the list is "bsp.test.no.matched.test.classes"
    val matchedClassesList = ListBox.create(matchedClassesModel)

    val refreshClassesButton = Button.create(LocalizeValue.of(BspBundle.message("bsp.test.refresh")), _ => fetchAndRefreshClasses())
    refreshClassesButton.setIcon(PlatformIconGroup.actionsRefresh())

    testClassFormWrapper = VerticalLayout.create()
    testClassFormWrapper.add(LabeledBuilder.filled(LocalizeValue.of(BspBundle.message("bsp.test.classes.regex")), testClassNameRegex))
    testClassFormWrapper.add(LabeledBuilder.filled(LocalizeValue.of(BspBundle.message("bsp.test.matched.classes")), matchedClassesList))
    testClassFormWrapper.add(refreshClassesButton)

    val mainPanel = VerticalLayout.create()
    mainPanel.add(LabeledBuilder.sided(LocalizeValue.of(BspBundle.message("bsp.test.kind")), testModeCombobox))
    mainPanel.add(testClassFormWrapper)
    mainPanel
  }

  @RequiredUIAccess
  private def onClassNameRegexChanged(): Unit = {
    val matched =
      try calculateMatchedClasses()
      catch { case _: PatternSyntaxException => Map.empty[String, List[String]] }
    updateMatchedClassesList(matched)
  }

  private def fetchAndRefreshClasses(): Unit =
    new FetchScalaTestClassesTask(project, xs => {
      lastRequestResult = xs.asScala.map(x => new TestClass(x.getTarget.getUri, x.getClasses)).asJava
      Application.get().getLastUIAccess.give((() => updateMatchedClassesList(calculateMatchedClasses())): Runnable)
    }, _.printStackTrace()).queue()

  @RequiredUIAccess
  private def onSelectModeChanged(current: TestMode): Unit = current match {
    case TestMode.ALL_IN_PROJECT => testClassFormWrapper.setVisible(false)
    case TestMode.CLASS =>
      if (lastRequestResult.isEmpty)
        fetchAndRefreshClasses()
      testClassFormWrapper.setVisible(true)
  }

  @RequiredUIAccess
  private def updateMatchedClassesList(matchedClasses: Map[String, List[String]]): Unit = {
    matchedClassesModel.removeAll()
    matchedClasses.values.flatten.toSeq.sorted.foreach(matchedClassesModel.add)
  }

  @RequiredUIAccess
  override def reset(conf: BspTestRunConfiguration): Unit = {
    testModeCombobox.setValue(conf.testMode)
    onSelectModeChanged(conf.testMode)
    testClassNameRegex.setValue(conf.testClassesRegex)
    lastRequestResult = conf.lastTestClassesResponse
    if (lastRequestResult.isEmpty && conf.testMode == TestMode.CLASS) fetchAndRefreshClasses()
    else updateMatchedClassesList(calculateMatchedClasses())
  }

  private def calculateMatchedClasses(): Map[String, List[String]] = {
    val regex = Pattern.compile(Option(testClassNameRegex.getValue).getOrElse(""))
    lastRequestResult.asScala.toList
      .flatMap(item => item.classes.asScala.filter(x => regex.matcher(x).matches()).map(item.target -> _))
      .groupMap(_._1)(_._2)
  }

  @RequiredUIAccess
  override def apply(runConfig: BspTestRunConfiguration): Unit = {
    var matched = Map.empty[String, List[String]]
    if (testModeCombobox.getValue == TestMode.CLASS) {
      if (lastRequestResult == null)
        throw new ConfigurationException(LocalizeValue.localizeTODO("No matched test classes"))
      matched =
        try calculateMatchedClasses()
        catch { case _: PatternSyntaxException => throw new ConfigurationException(LocalizeValue.localizeTODO("Illegal regex")) }
      if (matched.isEmpty)
        throw new ConfigurationException(LocalizeValue.localizeTODO("No matched test classes"))
    }

    // Apply UI to run config
    runConfig.lastTestClassesResponse = lastRequestResult
    runConfig.testClassesRegex = testClassNameRegex.getValue
    runConfig.matchedTestClasses = matched.view.mapValues(_.asJava).toMap.asJava
    runConfig.testMode = testModeCombobox.getValue
  }
}

object BspTestConfigurationForm {
  enum TestMode(val displayText: String) extends java.lang.Enum[TestMode] {
    case ALL_IN_PROJECT extends TestMode(BspBundle.message("bsp.test.all.in.project"))
    case CLASS extends TestMode(BspBundle.message("bsp.test.scala.class"))

    override def toString: String = displayText
  }
}
