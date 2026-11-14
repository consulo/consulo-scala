package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel
import consulo.project.Project
import consulo.disposer.Disposer
import consulo.language.codeStyle.CodeStyleSettings
import consulo.localize.LocalizeValue
import consulo.ui.{ComboBox, Component, Space}
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.DockLayout
import consulo.ui.util.LabeledBuilder
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.lang.rearranger.ScalaArrangementPanel
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.ScalaLanguage

import java.awt.Dimension
import javax.swing.JComponent
import scala.annotation.nowarn

class ScalaTabbedCodeStylePanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings)
  extends TabbedLanguageCodeStylePanel(ScalaLanguage.INSTANCE, currentSettings, settings) {

  import ScalaTabbedCodeStylePanel._

  private var formatterSelectorComboBox: ComboBox[String] = scala.compiletime.uninitialized
  private var outerLayout: DockLayout = scala.compiletime.uninitialized
  private var outerPanel: JComponent = scala.compiletime.uninitialized
  private var innerComponent: Component = scala.compiletime.uninitialized
  private var shortenedComponent: Component = scala.compiletime.uninitialized
  private var shortenedPanel: ScalafmtTabbedLanguageCodeStylePanel = scala.compiletime.uninitialized
  // TODO: rework this whole project juggling mess, there should be a straightforward way of depending on a project
  //  from code style settings panels
  private var typeAnnotationsPanel: TypeAnnotationsPanel = scala.compiletime.uninitialized

  override def dispose(): Unit = {
    super.dispose()
    Disposer.dispose(shortenedPanel)
  }

  override def getPanel: JComponent = outerPanel

  private def innerPanel: JComponent = super.getPanel

  override protected def initTabs(settings: CodeStyleSettings): Unit = {
    super.initTabs(settings)
    addTab(new ScalaDocFormattingPanel(settings))
    addTab(new ImportsPanel(settings))
    addTab(new MultiLineStringCodeStylePanel(settings))
    addTab({typeAnnotationsPanel = new TypeAnnotationsPanel(settings); typeAnnotationsPanel})
    addTab(new ScalaArrangementPanel(settings))
    addTab(new CodeGenerationPanel(settings))
    addTab(new OtherCodeStylePanel(settings))
    initOuterFormatterPanel()
  }

  override def isModified(settings: CodeStyleSettings): Boolean = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])

    val formatterTypeChanged = scalaSettings.FORMATTER != getSelectedFormatterId
    formatterTypeChanged ||
      super.isModified(settings) ||
      shortenedPanel.exposeIsModified(settings)
  }

  private def getSelectedFormatterId: Int =
    formatters(formatterSelectorComboBox.getValue)

  override def apply(settings: CodeStyleSettings): Unit = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])

    scalaSettings.FORMATTER = getSelectedFormatterId

    if (scalaSettings.USE_SCALAFMT_FORMATTER) {
      shortenedPanel.exposeApply(settings)
    } else {
      super.apply(settings)
    }

    syncPanels(scalaSettings.USE_SCALAFMT_FORMATTER)
  }

  override def resetImpl(settings: CodeStyleSettings): Unit = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])

    val formatterKey = formatters.find(_._2 == scalaSettings.FORMATTER).map(_._1).get
    formatterSelectorComboBox.setValue(formatterKey)

    toggleSettingsVisibility(scalaSettings.USE_SCALAFMT_FORMATTER)

    if (scalaSettings.USE_SCALAFMT_FORMATTER) {
      shortenedPanel.exposeResetImpl(settings)
    } else {
      super.resetImpl(settings)
    }

    syncPanels(scalaSettings.USE_SCALAFMT_FORMATTER)
  }

  private def initOuterFormatterPanel(): Unit = {
    formatterSelectorComboBox = ComboBox.create(formatters.keys.toSeq*)
    formatterSelectorComboBox.setValue(formatters.keys.head)
    formatterSelectorComboBox.addValueListener { _ =>
      //USE_SCALAFMT_FORMATTER setting is immediately set to allow proper formatting for core formatter examples
      val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
      val oldFormatter = scalaSettings.FORMATTER
      scalaSettings.FORMATTER = getSelectedFormatterId
      if (scalaSettings.FORMATTER != oldFormatter) {
        toggleSettingsVisibility(scalaSettings.USE_SCALAFMT_FORMATTER)
        syncPanels(scalaSettings.USE_SCALAFMT_FORMATTER)
      }
    }

    val formatterSelectorPanel = LabeledBuilder.simple(LocalizeValue.of(ScalaBundle.message("scala.root.code.style.panel.formatter")), formatterSelectorComboBox)
    formatterSelectorPanel.paddingBuilder().leftSet(Space.MEDIUM).apply()

    shortenedPanel = new ScalafmtTabbedLanguageCodeStylePanel(currentSettings, settings)
    // the tabs are the platform Swing tabbed panels
    innerComponent = TargetAWT.wrap(innerPanel)
    shortenedComponent = TargetAWT.wrap(shortenedPanel.getPanel)
    // this is required in order scrolling works correctly with small settings window height
    // otherwise on small screens the settings can be practically unusable
    innerPanel.setMinimumSize(new Dimension(1, 1))

    outerLayout = DockLayout.create()
    outerLayout.top(formatterSelectorPanel)
    outerPanel = TargetAWT.to(outerLayout).asInstanceOf[JComponent]

    toggleSettingsVisibility(false)
  }

  def onProjectSet(project: Project): Unit = {
    shortenedPanel.onProjectSet(project)
    typeAnnotationsPanel.onProjectSet(project)
  }

  // scalaFmtSettingsPanel.setModel should be called in order that its settings are saved properly
  // setModel in TabbedLanguageCodeStylePanel is final so we can't override and have to use workaround method
  def onModelSet(model: CodeStyleSchemesModel): Unit = shortenedPanel.setModel(model)

  private def syncPanels(useExternalFormatter: Boolean): Unit = {
    val tempSettings = settings.clone(): @nowarn("cat=deprecation")
    if (useExternalFormatter) {
      shortenedPanel.exposeApply(tempSettings)
      // we need to invoke applySettingsToModel, which is done inside onSomethingChanged
      shortenedPanel.onSomethingChanged()
      super.resetImpl(tempSettings)
    } else {
      super.apply(tempSettings)
      shortenedPanel.exposeResetImpl(tempSettings)
    }
  }

  private def toggleSettingsVisibility(useExternalFormatter: Boolean): Unit = {
    outerLayout.center(if (useExternalFormatter) shortenedComponent else innerComponent)
  }
}

object ScalaTabbedCodeStylePanel {
  @NonNls
  private val formatters: Map[String, Int] = Map(
    "IntelliJ" -> ScalaCodeStyleSettings.INTELLIJ_FORMATTER,
    "Scalafmt" -> ScalaCodeStyleSettings.SCALAFMT_FORMATTER,
  )

  private class ScalafmtTabbedLanguageCodeStylePanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings)
    extends TabbedLanguageCodeStylePanel(ScalaLanguage.INSTANCE, currentSettings, settings) {

    private var scalaFmtSettingsPanel: ScalaFmtSettingsPanel = scala.compiletime.uninitialized

    override protected def initTabs(settings: CodeStyleSettings): Unit = {
      scalaFmtSettingsPanel = new ScalaFmtSettingsPanel(settings)
      addTab(scalaFmtSettingsPanel)
      addTab(new ImportsPanel(settings))
      addTab(new MultiLineStringCodeStylePanel(settings))
      addTab(new TypeAnnotationsPanel(settings))
      addTab(new ScalaArrangementPanel(settings))
      addTab(new CodeGenerationPanel(settings))
      val otherCodeStylePanel: OtherCodeStylePanel = new OtherCodeStylePanel(settings)
      addTab(otherCodeStylePanel)
      otherCodeStylePanel.toggleExternalFormatter(true)
    }

    def exposeIsModified(settings: CodeStyleSettings): Boolean = super.isModified(settings)

    def exposeApply(settings: CodeStyleSettings): Unit = super.apply(settings)

    def exposeResetImpl(settings: CodeStyleSettings): Unit = super.resetImpl(settings)

    def onProjectSet(project: Project): Unit = scalaFmtSettingsPanel.onProjectSet(project)
  }
}
