package org.jetbrains.plugins.scala.testingSupport
package test.ui

import com.intellij.java.execution.impl.ui.ClassBrowser
import com.intellij.java.language.impl.JavaFileType
import com.intellij.java.language.impl.ui.{JavaReferenceEditorUtil, PackageChooserFactory}
import com.intellij.java.language.psi.{JavaCodeFragment, PsiClass}
import com.intellij.java.language.util.ClassFilter
import consulo.application.concurrent.coroutine.ReadLock
import consulo.document.Document
import consulo.execution.configuration.fragment.{RunConfigurationFragmentHost, StandardRunConfigurationFragmentId}
import consulo.execution.localize.ExecutionLocalize
import consulo.language.editor.ui.{EditorBox, EditorBoxBuilderFactory}
import consulo.language.psi.scope.GlobalSearchScope
import consulo.localize.LocalizeValue
import consulo.module.Module
import consulo.platform.base.icon.PlatformIconGroup
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.action.{ActionGroup, ActionToolbar, ActionToolbarFactory, AnAction, AnActionEvent, AnActionWithSyncUpdate, DumbAwareAction}
import consulo.ui.image.Image
import consulo.ui.layout.HorizontalLayout
import consulo.ui.util.FormBuilder
import consulo.ui.{CheckBox, ComboBox, Component, HasSuffixComponent, Label, TextArea, UIAction}
import consulo.util.concurrent.coroutine.{Coroutine, CoroutineScope}
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.testingSupport.test._
import org.jetbrains.plugins.scala.testingSupport.test.testdata._
import org.jetbrains.plugins.scala.testingSupport.test.ui.TestRunConfigurationForm._
import org.jetbrains.sbt.settings.SbtSettings

import scala.jdk.CollectionConverters._

/**
 * upstream: Swing form with the common java options (VM options, program arguments, working directory, environment
 * variables, module, JRE, shorten command line). In Consulo these are edited by the platform run configuration fragments,
 * see [[AbstractTestRunConfigurationFragment]]; the form keeps the test specific options.
 */
final class TestRunConfigurationForm @RequiredUIAccess() (val myProject: Project) {

  private var mySuitePaths: collection.Seq[String] = Seq.empty
  private var myModule    : Module                 = null
  // the module selector is the platform module fragment of the same page
  private var myFragmentHost: Option[RunConfigurationFragmentHost] = None

  // TOP PANEL: TestKind-specific
  private val myTestKind: ComboBox[TestKind] = {
    val box = ComboBox.create(TestKind.values().toSeq.asJava)
    box.setTextRenderer(kind => if (kind == null) LocalizeValue.empty() else testKindName(kind))
    box
  }
  private val myClass        : EditorBox    = createReferenceField(classesAccepted = true)
  private val myTestName     : TextArea     = TextArea.create()
  private val myPackage      : EditorBox    = createReferenceField(classesAccepted = false)
  private val mySearchForTest: ComboBox[SearchForTest] = {
    val box = ComboBox.create(SearchForTest.values().toSeq.asJava)
    box.setTextRenderer(scope => if (scope == null) LocalizeValue.empty() else searchForTestName(scope))
    box
  }
  private val myRegex           : RegexpPanel = new RegexpPanel
  private val myUseSbtCheckBox  : CheckBox    = CheckBox.create(LocalizeValue.of(TestingSupportBundle.message("test.run.config.use.sbt")))
  private val myUseUiWithSbt    : CheckBox    = CheckBox.create(LocalizeValue.of(TestingSupportBundle.message("test.run.config.use.ui.with.sbt")))

  // BOTTOM PANEL: common options
  private val myShowProgressMessagesCheckBox: CheckBox =
    CheckBox.create(LocalizeValue.of(TestingSupportBundle.message("test.run.config.print.information.messages.to.console")))

  private val builder = FormBuilder.create()
  private val myClassRow         = Row.add(builder, TestingSupportBundle.message("test.run.config.test.class"), myClass)
  private val myTestNameRow      = Row.add(builder, TestingSupportBundle.message("test.run.config.test.name"), myTestName)
  private val myPackageRow       = Row.add(builder, TestingSupportBundle.message("test.run.config.test.package"), myPackage)
  private val mySearchForTestRow = Row.add(builder, TestingSupportBundle.message("test.run.config.search.for.tests"), mySearchForTest)
  private val myRegexRow         = Row.add(builder, TestingSupportBundle.message("test.run.config.regular.expressions"), myRegex.getComponent)
  private val myForm: Component = builder.build()

  init()

  @RequiredUIAccess
  private def init(): Unit = {
    if (!myProject.getApplication.isUnifiedApplication)
      installChooserActions()

    myTestKind.addValueListener(_ => UiComponentsVisibility.update())
    mySearchForTest.addValueListener(_ => UiComponentsVisibility.update())
    myUseSbtCheckBox.addValueListener(_ => UiComponentsVisibility.update())

    myTestKind.setValue(TestKind.CLAZZ, false)
    mySearchForTest.setValue(SearchForTest.ACCROSS_MODULE_DEPENDENCIES, false)
    UiComponentsVisibility.update()
  }

  def testKindComponent: Component = myTestKind

  def sbtOptionsComponent: Component = HorizontalLayout.create().add(myUseSbtCheckBox).add(myUseUiWithSbt)

  def formComponent: Component = myForm

  def showProgressMessagesComponent: Component = myShowProgressMessagesCheckBox

  @RequiredUIAccess
  def attach(host: RunConfigurationFragmentHost): Unit = {
    myFragmentHost = Some(host)
    UiComponentsVisibility.update()
  }

  private def isModuleSelectionSupported(testKind: TestKind, searchScope: SearchForTest): Boolean =
    (testKind, searchScope) match {
      case (TestKind.ALL_IN_PACKAGE, SearchForTest.IN_WHOLE_PROJECT) => false
      case _                                                         => true
    }

  @RequiredUIAccess
  def resetFrom(configuration: AbstractTestRunConfiguration): Unit = {
    val configurationData = configuration.testConfigurationData
    configurationData match {
      case data: AllInPackageTestData => setTestPackagePath(data.testPackagePath)
      case data: RegexpTestData       => setRegexps(data.regexps)
      case data: ClassTestData        =>
        data match {
          case singleTest: SingleTestData =>  setTestName(singleTest.testName)
          case _=>
        }
        setTestClassPath(data.testClassPath)
      case _ =>
    }

    setTestKind(configurationData.getKind)
    setSearchForTest(configurationData.searchTest)

    resetSbtOptionsFrom(configuration)

    myModule = configuration.getModule
    setShowProgressMessages(configurationData.showProgressMessages)

    mySuitePaths = configuration.javaSuitePaths.asScala

    UiComponentsVisibility.update(configuration)
  }

  @RequiredUIAccess
  def applyTo(configuration: AbstractTestRunConfiguration): Unit = {
    configuration.testKind = this.getTestKind
    myModule = configuration.getModule
    configuration.testConfigurationData = TestConfigurationData.createFromForm(this, configuration)
    configuration.testConfigurationData.initWorkingDirIfEmpty()
  }

  @RequiredUIAccess
  private def resetSbtOptionsFrom(configuration: AbstractTestRunConfiguration): Unit = {
    val configurationData = configuration.testConfigurationData
    setUseSbt(configurationData.useSbt)
    setUseUiWithSbt(configurationData.useUiWithSbt)
  }

  def getModule: Module = myModule

  def getTestKind: TestKind = Option(myTestKind.getValue).getOrElse(TestKind.CLAZZ)
  def getTestClassPath: String = text(myClass.getValue)
  def getTestName: String = StringUtil.notNullize(myTestName.getValue)
  def getTestPackagePath: String = text(myPackage.getValue)
  def getSearchForTest: SearchForTest = Option(mySearchForTest.getValue).getOrElse(SearchForTest.ACCROSS_MODULE_DEPENDENCIES)
  def getRegexps: (Array[String], Array[String]) = myRegex.getRegexps
  def getUseSbt: Boolean = myUseSbtCheckBox.getValueOrError
  def getUseUiWithSbt: Boolean = myUseUiWithSbt.getValueOrError
  def getShowProgressMessages: Boolean = myShowProgressMessagesCheckBox.getValueOrError

  @RequiredUIAccess private def setTestKind(kind: TestKind): Unit = myTestKind.setValue(kind, false)
  @RequiredUIAccess private def setTestClassPath(s: String): Unit = myClass.setValue(s)
  @RequiredUIAccess private def setTestName(s: String): Unit = myTestName.setValue(s, false)
  @RequiredUIAccess private def setTestPackagePath(s: String): Unit = myPackage.setValue(s)
  @RequiredUIAccess private def setSearchForTest(searchTest: SearchForTest): Unit = mySearchForTest.setValue(searchTest, false)
  @RequiredUIAccess private def setRegexps(classRegexps: Array[String], testRegexps: Array[String]): Unit = myRegex.setRegexps(classRegexps, testRegexps)
  @RequiredUIAccess private def setRegexps(regexps: (Array[String], Array[String])): Unit = setRegexps(regexps._1, regexps._2)
  @RequiredUIAccess private def setUseSbt(b: Boolean): Unit = myUseSbtCheckBox.setValue(b, false)
  @RequiredUIAccess private def setUseUiWithSbt(b: Boolean): Unit = myUseUiWithSbt.setValue(b, false)
  @RequiredUIAccess private def setShowProgressMessages(b: Boolean): Unit = myShowProgressMessagesCheckBox.setValue(b, false)

  private object UiComponentsVisibility {

    @RequiredUIAccess
    def update(): Unit = {
      val testKind = getTestKind
      hideAll()
      testKind match {
        case TestKind.CLAZZ          => setClassVisible(true)
        case TestKind.TEST_NAME      => setTestNameVisible(true)
        case TestKind.REGEXP         => setRegexpVisible(true)
        case TestKind.ALL_IN_PACKAGE => setPackageVisible(true)
      }

      val packageSearchScope = getSearchForTest
      myFragmentHost.foreach(_.setFragmentEnabled(StandardRunConfigurationFragmentId.MODULE, isModuleSelectionSupported(testKind, packageSearchScope)))
      myUseUiWithSbt.setEnabled(getUseSbt)
    }

    @RequiredUIAccess
    def update(configuration: AbstractTestRunConfiguration): Unit = {
      update()
      val sbtSupport = configuration.runStateProvider match {
        case provider: CustomTestRunnerOrSbtShellStateProvider => Some(provider.sbtSupport)
        case provider: SbtShellBasedStateProvider              => Some(provider.sbtSupport)
        case _                                                 => None
      }
      sbtSupport match {
        case Some(support ) =>
          val projectHasSbt = hasSbt(myProject)
          myUseSbtCheckBox.setVisible(projectHasSbt)
          myUseUiWithSbt.setVisible(projectHasSbt && support.allowsSbtUiRun)
        case None =>
          myUseSbtCheckBox.setVisible(false)
          myUseUiWithSbt.setVisible(false)
      }
    }

    @RequiredUIAccess
    private def hideAll(): Unit = {
      setPackageVisible(false)
      setClassVisible(false)
      setTestNameVisible(false)
      setRegexpVisible(false)
    }

    @RequiredUIAccess
    private def setClassVisible(visible: Boolean): Unit =
      myClassRow.setVisible(visible)

    @RequiredUIAccess
    private def setTestNameVisible(visible: Boolean): Unit = {
      myTestNameRow.setVisible(visible)
      myClassRow.setVisible(visible)
    }

    @RequiredUIAccess
    private def setPackageVisible(visible: Boolean): Unit = {
      myPackageRow.setVisible(visible)
      mySearchForTestRow.setVisible(visible)
    }

    @RequiredUIAccess
    private def setRegexpVisible(visible: Boolean): Unit =
      myRegexRow.setVisible(visible)
  }

  @RequiredUIAccess
  private def createReferenceField(classesAccepted: Boolean): EditorBox = {
    val field = myProject.getApplication.getInstance(classOf[EditorBoxBuilderFactory]).create(myProject).build()
    if (!myProject.isDefault) {
      CoroutineScope.launchAsync(
        myProject.coroutineContext(),
        () => Coroutine
          .first(ReadLock.apply[Void, Document] { _ =>
            JavaReferenceEditorUtil.createDocument("", myProject, classesAccepted, JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE)
          })
          .`then`(UIAction.apply[Document, Void] { document =>
            if (document != null) {
              val text = StringUtil.notNullize(field.getValue)
              field.setDocument(document, JavaFileType.INSTANCE)
              field.setValue(text)
            }
            null
          })
      )
    }
    field
  }

  @RequiredUIAccess
  private def installChooserActions(): Unit = {
    installAction(myPackage, "ScalaTestConfigurationPackage", new ChooserAction(
      myProject,
      ExecutionLocalize.choosePackageDialogTitle(),
      PlatformIconGroup.nodesPackage(),
      () => {
        val chooser = myProject.getInstance(classOf[PackageChooserFactory]).create()
        val packages = chooser.showAndSelect()
        if (packages != null && !packages.isEmpty)
          myPackage.setValue(packages.get(0).getQualifiedName)
      }
    ))

    val classBrowser = new MyClassBrowser(myProject)
    installAction(myClass, "ScalaTestConfigurationClass", new ChooserAction(
      myProject,
      LocalizeValue.of(TestingSupportBundle.message("test.run.config.choose.test.class")),
      PlatformIconGroup.nodesClass(),
      () => {
        val className = classBrowser.chooseClass(myClass.getValue)
        if (className != null)
          myClass.setValue(className)
      }
    ))
  }

  @RequiredUIAccess
  private def installAction[C <: Component & HasSuffixComponent](field: C, place: String, action: AnAction): Unit = {
    val toolbar = myProject.getApplication
      .getInstance(classOf[ActionToolbarFactory])
      .createActionToolbar(place, ActionGroup.newImmutableBuilder().add(action).build(), ActionToolbar.Style.INPLACE)
    toolbar.setTargetUIComponent(field)
    toolbar.updateActionsAsync()
    field.setSuffixComponent(toolbar.getUIComponent)
  }

  private class MyClassBrowser(project: Project)
    extends ClassBrowser(project, TestingSupportBundle.message("test.run.config.choose.test.class")) {

    override protected def getFilter: ClassFilter.ClassFilterWithScope = new ClassFilter.ClassFilterWithScope() {
      override def getScope: GlobalSearchScope =
        getModule match {
          case null   => GlobalSearchScope.allScope(project)
          case module => GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        }

      override def isAccepted(aClass: PsiClass): Boolean = {
        if (!getScope.accept(aClass.getContainingFile.getVirtualFile)) return false
        val baseSuite = findBaseSuiteClass(project, aClass)
        baseSuite.isDefined
      }

      private def findBaseSuiteClass(project: Project, aClass: PsiClass): Option[PsiClass] =
        (for {
          suitePath  <- mySuitePaths.iterator
          suiteClass <- ScalaPsiManager.instance(project).getCachedClasses(getScope, suitePath).iterator
          if ScalaPsiUtil.isInheritorDeep(aClass, suiteClass)
        } yield suiteClass).nextOption()
    }

    override protected def findClass(className: String): PsiClass = {
      val cachedClass = ScalaPsiManager.instance(project).getCachedClass(GlobalSearchScope.allScope(project), className)
      if (cachedClass.isEmpty) null
      else cachedClass.get
    }
  }
}

private object TestRunConfigurationForm {

  private def testKindName(kind: TestKind): LocalizeValue = LocalizeValue.of(kind match {
    case TestKind.ALL_IN_PACKAGE => TestingSupportBundle.message("test.run.config.test.kind.all.in.package")
    case TestKind.CLAZZ          => TestingSupportBundle.message("test.run.config.test.kind.class")
    case TestKind.TEST_NAME      => TestingSupportBundle.message("test.run.config.test.kind.test.name")
    case TestKind.REGEXP         => TestingSupportBundle.message("test.run.config.test.kind.regular.expression")
  })

  private def searchForTestName(scope: SearchForTest): LocalizeValue = LocalizeValue.of(scope match {
    case SearchForTest.IN_WHOLE_PROJECT            => TestingSupportBundle.message("test.run.config.search.scope.in.whole.project")
    case SearchForTest.IN_SINGLE_MODULE            => TestingSupportBundle.message("test.run.config.search.scope.in.single.module")
    case SearchForTest.ACCROSS_MODULE_DEPENDENCIES => TestingSupportBundle.message("test.run.config.search.scope.across.module.dependencies")
  })

  private def text(value: String): String = StringUtil.notNullize(value).trim

  private final class Row(label: Label, field: Component) {
    @RequiredUIAccess
    def setVisible(visible: Boolean): Unit = {
      label.setVisible(visible)
      field.setVisible(visible)
    }
  }

  private object Row {
    @RequiredUIAccess
    def add(builder: FormBuilder, text: String, field: Component): Row = {
      val label = Label.create(LocalizeValue.of(StringUtil.trimEnd(text.replace("&", ""), ":")))
      builder.addLabeled(label, field)
      new Row(label, field)
    }
  }

  private final class ChooserAction(project: Project, text: LocalizeValue, icon: Image, @RequiredUIAccess chooser: Runnable)
    extends DumbAwareAction(text, LocalizeValue.empty(), icon) with AnActionWithSyncUpdate {

    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(!project.isDefault)

    @RequiredUIAccess
    override def actionPerformed(e: AnActionEvent): Unit =
      chooser.run()
  }

  private def hasSbt(project: Project): Boolean = {
    val sbtSettings = SbtSettings.getInstance(project)
    val sbtSettingsEmpty = sbtSettings == null || sbtSettings.getLinkedProjectsSettings.isEmpty
    !sbtSettingsEmpty
  }
}
