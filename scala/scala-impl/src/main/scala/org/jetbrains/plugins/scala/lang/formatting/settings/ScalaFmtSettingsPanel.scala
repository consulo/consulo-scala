package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.platform.base.icon.PlatformIconGroup

import consulo.language.Language
import consulo.ui.ex.action.ActionManager
import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.colorScheme.EditorColorsManager
import consulo.codeEditor.EditorEx
import consulo.language.editor.highlight.LayeredLexerEditorHighlighter
import consulo.codeEditor.{EditorFactory, EditorSettings}
import consulo.fileChooser.FileChooserDescriptorFactory
import consulo.document.FileDocumentManager
import consulo.language.editor.highlight.SyntaxHighlighterFactory.getSyntaxHighlighter
import consulo.ui.ex.keymap.util.KeymapUtil
import consulo.project.Project
import consulo.ui.{NotificationType => MessageType}
import consulo.ui.ModalityState
import consulo.ui.{Button, CheckBox, Component, Label, LabelStyle, Space, TextBox}
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.TextComponentAccessor
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{DockLayout, HorizontalLayout, VerticalLayout}
import consulo.ui.style.ComponentColors
import consulo.ui.util.FormBuilder
import consulo.fileChooser.FileChooserTextBoxBuilder
import consulo.localize.LocalizeValue
import consulo.ui.ex.popup.{Balloon, JBPopupFactory}
import consulo.virtualFileSystem.VirtualFile
import consulo.language.codeStyle.CodeStyleSettings
import consulo.ui.ex.RelativePoint
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.{ObjectExt, inReadAction, inWriteAction, invokeAndWait, invokeLater}
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtDynamicConfigService.ConfigResolveError
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtDynamicConfigService.ConfigResolveError.{ConfigError, ConfigScalafmtResolveError}
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtDynamicService.DefaultVersion
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtNotifications.FmtVerbosity
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.utils.ScalafmtConfigUtils
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.{ScalafmtDynamicConfigService, ScalafmtDynamicConfigServiceImpl, ScalafmtDynamicService}
import org.scalafmt.dynamic.ScalafmtVersion

import java.awt.Point
import java.util.Collections.emptyList
import javax.swing.{JComponent, JPanel}

final class ScalaFmtSettingsPanel(settings: CodeStyleSettings) extends ScalaCodeStylePanelBase(settings, "Scalafmt") {

  import ScalaFmtSettingsPanel._

  private val Log = Logger.getInstance(getClass)

  override val getEditor: EditorEx = createConfigEditor

  override def dispose(): Unit = {
    super.dispose()
    if (getEditor != null) {
      EditorFactory.getInstance().releaseEditor(getEditor)
    }
  }

  override def apply(settings: CodeStyleSettings): Unit = {
    val editorText = getEditor.getDocument.getText
    val configTextChangedInEditor = configText.exists(_ != editorText)

    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    val panelWasJustEnabled = isPanelEnabled != scalaSettings.USE_SCALAFMT_FORMATTER

    val modified = isModified(settings) || configTextChangedInEditor || panelWasJustEnabled
    if (!modified) return

    scalaSettings.SCALAFMT_SHOW_INVALID_CODE_WARNINGS = showScalaFmtInvalidCodeWarnings.getValue.booleanValue
    scalaSettings.SCALAFMT_USE_INTELLIJ_FORMATTER_FOR_RANGE_FORMAT = useIntellijFormatterForRangeFormat.getValue.booleanValue
    scalaSettings.SCALAFMT_REFORMAT_ON_FILES_SAVE = reformatOnFileSaveCheckBox.getValue.booleanValue
    scalaSettings.SCALAFMT_FALLBACK_TO_DEFAULT_SETTINGS = fallBackToDefaultSettings.getValue.booleanValue

    val configPath = scalaSettings.SCALAFMT_CONFIG_PATH.trim
    val configPathNew = configPathFromUi
    val configPathChanged = configPath != configPathNew

    projectConfigFile(configPathNew) match {
      case Some(vFile) =>
        if (configTextChangedInEditor) {
          saveConfigChangesToFile(editorText, vFile)
        }

        if (configPathChanged || panelWasJustEnabled) {
          scalaSettings.SCALAFMT_CONFIG_PATH = configPathNew
          updateConfigTextFromFile(vFile)
        }
        ensureScalafmtResolvedAndUpdateVersionLabel(vFile)
      case None =>
        if (configPathChanged || configTextChangedInEditor) {
          if (configPathNew.isEmpty) {
            scalaSettings.SCALAFMT_CONFIG_PATH = configPathNew
            configText = None
          } else {
            reportConfigFileNotFound(configPathNew)
          }
        }
        ensureDefaultScalafmtResolved()
    }

    isPanelEnabled = scalaSettings.USE_SCALAFMT_FORMATTER
    updateConfigVisibility()
    updateUseIntellijWarningVisibility(scalaSettings)
  }

  private def configPathFromUi: String =
    Option(externalFormatterSettingsPath.getComponent.getValue).getOrElse("").trim

  //noinspection HardCodedStringLiteral
  private def updateScalafmtVersionLabel(version: String, isDefault: Boolean = false): Unit = {
    if (scalafmtVersionLabel == null) return
    val text = version + (if (isDefault) s" (${ScalaBundle.message("scalafmt.default")})" else "")
    // the version can be resolved asynchronously
    invokeLater(ModalityState.any())(scalafmtVersionLabel.setText(text))
  }

  private def ensureScalafmtResolvedAndUpdateVersionLabel(configFile: VirtualFile): Unit = {
    val project = projectOpt match {
      case Some(value) => value
      case None => return
    }

    val versionOpt = ScalafmtDynamicConfigServiceImpl.readVersion(project, configFile) match {
      case Right(v) => v
      case Left(ex) =>
        reportConfigParseError(ex.getMessage)
        return
    }
    val version = versionOpt.getOrElse(DefaultVersion)

    ScalafmtDynamicConfigService.instanceIn(project).toOption match {
      case Some(configManager) =>
        configManager.resolveConfigAsync(configFile, version, FmtVerbosity.Silent, onResolveFinished = {
          case Right(config) =>
            updateScalafmtVersionLabel(config.getVersion.toString(), isDefault = versionOpt.isEmpty)
          case Left(error: ConfigError) =>
            updateScalafmtVersionLabel(version.toString(), isDefault = versionOpt.isEmpty)
            reportConfigResolveError(error)
          case Left(error: ConfigResolveError) =>
            updateScalafmtVersionLabel("")
            reportConfigResolveError(error)
        })
      case _ =>
        Log.assertTrue(project.isDefault, "Config manager is expected to be missing only in default projects")
        ScalafmtDynamicService.instance.resolveAsync(version, project, {
          case Right(scalaFmtReflect) =>
            updateScalafmtVersionLabel(scalaFmtReflect.version.toString(), isDefault = versionOpt.isEmpty)
          case Left(error: ScalafmtDynamicService.ScalafmtResolveError) =>
            updateScalafmtVersionLabel("")
            reportConfigResolveError(ConfigScalafmtResolveError(error))
        })
    }
  }

  private def ensureDefaultScalafmtResolved(): Unit = {
    if (projectOpt.isEmpty) return
    ScalafmtDynamicService.instance.resolveAsync(DefaultVersion, projectOpt.get)
    updateScalafmtVersionLabel(DefaultVersion.toString(), isDefault = true)
  }

  private def reportConfigResolveError(configResolveError: ConfigResolveError): Unit = {
    import ConfigResolveError._
    configResolveError match {
      case ConfigFileNotFound(configPath) => reportConfigFileNotFound(configPath)
      case ConfigParseError(_, exception) => reportConfigParseError(exception.getMessage)
      case ConfigCyclicDependenciesError(_, _) => reportConfigParseError(ScalaBundle.message("scalafmt.config.load.errors.cyclic.includes.detected"))
      case ConfigScalafmtResolveError(error) => reportCantResolveVersion(error.version)
    }
  }

  private def reportConfigParseError(errorMessage: String): Unit = {
    val component = previewPanel
    displayMessage(
      ScalaBundle.message("scalafmt.settings.panel.errors.failed.to.parse.config", errorMessage),
      component, component.getWidth - 10, component.getHeight - 55, Balloon.Position.above,
      MessageType.WARNING
    )
  }

  private def reportCantResolveVersion(version: ScalafmtVersion): Unit = {
    val component = previewPanel
    displayMessage(
      ScalaBundle.message("scalafmt.settings.panel.errors.cant.resolve.version", version),
      component, component.getWidth - 10, component.getHeight - 55, Balloon.Position.above,
      MessageType.WARNING
    )
  }

  private def reportConfigFileNotFound(configPath: String): Unit = {
    val component = TargetAWT.to(externalFormatterSettingsPath.getComponent).asInstanceOf[JComponent]
    displayMessage(
      ScalaBundle.message("scalafmt.settings.panel.errors.cant.find.config.file", configPath),
      component, component.getWidth / 2, component.getHeight, Balloon.Position.below,
      MessageType.ERROR
    )
  }

  private def displayMessage(@Nls text: String, relativeTo: JComponent,
                             xPosition: Int, yPosition: Int,
                             direction: Balloon.Position,
                             messageType: MessageType): Unit = {
    val factory = JBPopupFactory.getInstance.createHtmlTextBalloonBuilder(text, messageType, null)
    val balloon = factory.createBalloon()
    val balloonPosition = new RelativePoint(relativeTo, new Point(xPosition, yPosition))
    balloon.show(balloonPosition, direction)
  }

  private def updateConfigTextFromFile(vFile: VirtualFile): Unit = {
    configText = inReadAction(FileDocumentManager.getInstance.getDocument(vFile).toOption.map(_.getText))
    configText.foreach(text => inWriteAction {
      getEditor.getDocument.setText(text)
    })
  }

  private def saveConfigChangesToFile(configTextNew: String, vFile: VirtualFile): Unit = {
    val document = inReadAction(FileDocumentManager.getInstance.getDocument(vFile))
    inWriteAction {
      invokeAndWait(ApplicationManager.getApplication.getCurrentModalityState) {
        document.setText(configTextNew)
      }
    }
    configText = Some(configTextNew)
  }

  override def isModified(settings: CodeStyleSettings): Boolean = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    scalaSettings.SCALAFMT_CONFIG_PATH.trim != configPathFromUi ||
      scalaSettings.SCALAFMT_SHOW_INVALID_CODE_WARNINGS != showScalaFmtInvalidCodeWarnings.getValue.booleanValue ||
      scalaSettings.SCALAFMT_USE_INTELLIJ_FORMATTER_FOR_RANGE_FORMAT != useIntellijFormatterForRangeFormat.getValue.booleanValue ||
      scalaSettings.SCALAFMT_REFORMAT_ON_FILES_SAVE != reformatOnFileSaveCheckBox.getValue.booleanValue ||
      scalaSettings.SCALAFMT_FALLBACK_TO_DEFAULT_SETTINGS != fallBackToDefaultSettings.getValue.booleanValue ||
      configText.exists(_ != getEditor.getDocument.getText)
  }

  override def resetImpl(codeStyleSettings: CodeStyleSettings): Unit = {
    val scalaSettings = codeStyleSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    val configPath = scalaSettings.SCALAFMT_CONFIG_PATH.trim

    externalFormatterSettingsPath.getComponent.setValue(configPath, false)
    showScalaFmtInvalidCodeWarnings.setValue(scalaSettings.SCALAFMT_SHOW_INVALID_CODE_WARNINGS)
    useIntellijFormatterForRangeFormat.setValue(scalaSettings.SCALAFMT_USE_INTELLIJ_FORMATTER_FOR_RANGE_FORMAT)
    reformatOnFileSaveCheckBox.setValue(scalaSettings.SCALAFMT_REFORMAT_ON_FILES_SAVE)
    fallBackToDefaultSettings.setValue(scalaSettings.SCALAFMT_FALLBACK_TO_DEFAULT_SETTINGS)
    // upstream: also moved the focus to the browse button of the config path field

    isPanelEnabled = scalaSettings.USE_SCALAFMT_FORMATTER
    if (isPanelEnabled) {
      projectConfigFile(configPath) match {
        case Some(vFile) =>
          updateConfigTextFromFile(vFile)
          ensureScalafmtResolvedAndUpdateVersionLabel(vFile)
        case None =>
          ensureDefaultScalafmtResolved()
      }
    }

    updateConfigVisibility()
    updateUseIntellijWarningVisibility(scalaSettings)
  }

  override protected def getPanelInner: Component = {
    if (myPanel == null) {
      myPanel = buildInnerPanel
    }
    myPanel
  }

  private def createNewConfigFileAndUpdate(): Unit =
    projectOpt match {
      case Some(project) =>
        val configPath = ScalafmtConfigUtils.actualConfigPath(configPathFromUi)
        ScalafmtDynamicConfigServiceImpl.createConfigurationFile(project, configPath, openInEditor = true) match {
          case Left(None) =>
          case Left(Some(ex)) =>
            Log.warn("Couldn't create configuration file: " + ex)
          case Right(vFile) =>
            updateConfigTextFromFile(vFile)
            updateConfigVisibility()
            ensureScalafmtResolvedAndUpdateVersionLabel(vFile)
        }
      case None =>
    }

  @RequiredUIAccess
  private def buildInnerPanel: Component = {
    showScalaFmtInvalidCodeWarnings = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.show.warnings.when.formatting.invalid.code")))
    useIntellijFormatterForRangeFormat = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.use.intellij.formatter.for.code.range.formatting")))
    useIntellijWarning = Label.create()
    useIntellijWarning.setImage(PlatformIconGroup.generalWarning())
    useIntellijWarning.setToolTipText(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.use.intellij.formatter.for.code.range.formatting.warning.tooltip")))
    useIntellijFormatterForRangeFormat.addValueListener { _ =>
      useIntellijWarning.setVisible(!useIntellijFormatterForRangeFormat.getValue)
    }
    val useIntellijFormatterWrapper = HorizontalLayout.create(Space.X_SMALL)
      .add(useIntellijFormatterForRangeFormat)
      .add(useIntellijWarning)

    def saveActionsShortcutsText: String = {
      val actionManager = ActionManager.getInstance

      val saveAllShortcut = Option(actionManager.getKeyboardShortcut("SaveAll"))
      val saveDocumentShortcut = Option(actionManager.getKeyboardShortcut("SaveDocument"))

      val shortcuts = saveAllShortcut.toSeq ++ saveDocumentShortcut
      val shortcutTexts = shortcuts.map(KeymapUtil.getShortcutText)
      shortcutTexts.mkString(", ")
    }

    reformatOnFileSaveCheckBox = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.reformat.on.file.save", saveActionsShortcutsText)))
    // upstream: ComponentPanelBuilder.createCommentComponent
    val reformatOnFileSaveComment = Label.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.reformat.on.file.save.tooltip", KeymapUtil.getFirstKeyboardShortcutText("ReformatCode"))))
    reformatOnFileSaveComment.addStyle(LabelStyle.FONT_SMALL)
    reformatOnFileSaveComment.setForegroundColor(ComponentColors.DISABLED_TEXT)
    fallBackToDefaultSettings = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.fallback.to.default.settings")))

    externalFormatterSettingsPath = createConfigPathField()

    scalafmtVersionLabel = Label.create()

    noConfigLabel = Label.create(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.no.config.found.under.path.using.default")))
    createConfigButton = Button.create(LocalizeValue.of(ScalaBundle.message("scalafmt.can.not.find.config.file.create.new")), _ => createNewConfigFileAndUpdate())
    noConfigGroup = HorizontalLayout.create(Space.MEDIUM).add(noConfigLabel).add(createConfigButton)

    // the config editor preview is the platform Swing panel
    previewPanel = new JPanel()
    installPreviewPanel(previewPanel)
    getEditor.getComponent.setPreferredSize(TargetAWT.to(noConfigGroup).getPreferredSize)

    val form = FormBuilder.create()
      .addLabeled(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.configuration.label") + ":"), externalFormatterSettingsPath.getComponent)
      .addLabeled(LocalizeValue.of(ScalaBundle.message("scalafmt.settings.panel.scalafmt.version.label") + ":"), scalafmtVersionLabel)
      .build()

    val options = VerticalLayout.create()
      .add(showScalaFmtInvalidCodeWarnings)
      .add(useIntellijFormatterWrapper)
      .add(HorizontalLayout.create().add(reformatOnFileSaveCheckBox).add(reformatOnFileSaveComment))
      .add(fallBackToDefaultSettings)
      .add(form)
      .add(noConfigGroup)

    val inner = DockLayout.create(Space.MEDIUM)
      .top(options)
      .center(TargetAWT.wrap(previewPanel))
    inner.paddingBuilder().verticalSet(Space.MEDIUM).horizontalSet(Space.LARGE).apply()

    updateConfigVisibility(configVisible = true)

    inner
  }

  private def updateConfigPath(configPath: String): Unit = {
    projectConfigFile(configPath) match {
      case Some(vFile) =>
        updateConfigTextFromFile(vFile)
        ensureScalafmtResolvedAndUpdateVersionLabel(vFile)
      case _ =>
        configText = None
        ensureDefaultScalafmtResolved()
    }
    updateConfigVisibility()
  }

  // upstream: the browse listener was re-registered with the project when it was set
  @RequiredUIAccess
  private def createConfigPathField(): FileChooserTextBoxBuilder.Controller = {
    // if config path text field is empty we want to select default config file in project tree of file browser
    val textAccessor = new TextComponentAccessor[TextBox]() {
      override def setValue(textField: TextBox, text: String, fireListeners: Boolean): Unit = {
        textField.setValue(text, fireListeners)
        updateConfigPath(text)
      }

      override def getValue(textField: TextBox): String = {
        val path = ScalafmtConfigUtils.actualConfigPath(Option(textField.getValue).getOrElse(""))
        val absolutePath = projectOpt.flatMap(ScalafmtConfigUtils.projectConfigFileAbsolutePath(_, path))
        absolutePath.getOrElse(DefaultConfigFilePath)
      }
    }

    val controller = FileChooserTextBoxBuilder.create(null)
      .dialogTitle(LocalizeValue.of(customSettingsTitle))
      .dialogDescription(LocalizeValue.of(customSettingsTitle))
      .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withExtensionFilter("conf"))
      .textBoxAccessor(textAccessor)
      .build()

    val textBox = controller.getComponent
    //noinspection HardCodedStringLiteral
    textBox.setPlaceholder(LocalizeValue.of(ScalaBundle.message("scalafmt.default.path", DefaultConfigFilePath)))

    // if we typed only whitespaces and lost focus from text field we should display empty text placeholder
    textBox.addBlurListener { _ =>
      val configPath = configPathFromUi
      if (StringUtils.isBlank(configPath)) {
        textBox.setValue(null, false)
        updateConfigPath(DefaultConfigFilePath)
      } else {
        updateConfigPath(configPath)
      }
    }

    controller
  }

  def onProjectSet(aProject: Project): Unit = {
    projectOpt = Some(aProject)
    resetImpl(settings)
  }

  private def updateUseIntellijWarningVisibility(settings: ScalaCodeStyleSettings): Unit = {
    if (useIntellijWarning == null) return
    useIntellijWarning.setVisible(!settings.SCALAFMT_USE_INTELLIJ_FORMATTER_FOR_RANGE_FORMAT)
  }

  private def updateConfigVisibility(): Unit =
    updateConfigVisibility(configText.isDefined)

  private def updateConfigVisibility(configVisible: Boolean): Unit = {
    previewPanel.setVisible(configVisible)
    noConfigGroup.setVisible(!configVisible)
  }

  private def projectConfigFile(configPath: String): Option[VirtualFile] =
    projectOpt.flatMap(ScalafmtConfigUtils.projectConfigFile(_, configPath))

  private var isPanelEnabled: Boolean = false
  private var projectOpt: Option[Project] = None
  private var configText: Option[CharSequence] = None
  private var scalafmtVersionLabel: Label = scala.compiletime.uninitialized
  private var noConfigGroup: Component = scala.compiletime.uninitialized
  private var noConfigLabel: Label = scala.compiletime.uninitialized
  private var createConfigButton: Button = scala.compiletime.uninitialized
  private var previewPanel: JPanel = scala.compiletime.uninitialized
  private var myPanel: Component = scala.compiletime.uninitialized
  private var externalFormatterSettingsPath: FileChooserTextBoxBuilder.Controller = scala.compiletime.uninitialized
  private var showScalaFmtInvalidCodeWarnings: CheckBox = scala.compiletime.uninitialized
  private var useIntellijFormatterForRangeFormat: CheckBox = scala.compiletime.uninitialized
  private var useIntellijWarning: Label = scala.compiletime.uninitialized
  private var reformatOnFileSaveCheckBox: CheckBox = scala.compiletime.uninitialized
  private var fallBackToDefaultSettings: CheckBox = scala.compiletime.uninitialized
  private val customSettingsTitle = ScalaBundle.message("scalafmt.settings.panel.select.custom.scalafmt.configuration.file")
  //noinspection HardCodedStringLiteral
  private val DefaultConfigFilePath = s".${java.io.File.separatorChar}${ScalafmtDynamicConfigService.DefaultConfigurationFileName}"
}

object ScalaFmtSettingsPanel {

  object SearchFilter {
    // TODO: will not work in non-english localization, also see ScalaCompileServerForm
    def ScalafmtConfiguration = "Scalafmt Configuration"
  }

  //copied from CodeStyleAbstractPanel
  //using non-null getPreviewText breaks setting saving (!!!)
  private def createConfigEditor: EditorEx = {
    val editorFactory = EditorFactory.getInstance
    val editorDocument = editorFactory.createDocument("")
    val editor = editorFactory.createEditor(editorDocument)
      .asInstanceOf[EditorEx]

    val hoconLanguage = Language.findLanguageByID("HOCON")
    fillEditorSettings(editor.getSettings, hoconLanguage)

    if (hoconLanguage != null) {
      val highlighter = new LayeredLexerEditorHighlighter(
        getSyntaxHighlighter(hoconLanguage, null, null),
        EditorColorsManager.getInstance.getGlobalScheme // upstream: new DefaultColorsScheme
      )
      editor.setHighlighter(highlighter)
    }

    editor
  }

  private def fillEditorSettings(settings: EditorSettings,
                                       language: Language): Unit = {
    // upstream: settings.setLanguageSupplier(() => language)
    settings.setWhitespacesShown(true)
    settings.setLineMarkerAreaShown(false)
    settings.setIndentGuidesShown(false)
    settings.setLineNumbersShown(false)
    settings.setFoldingOutlineShown(false)
    settings.setAdditionalColumnsCount(0)
    settings.setAdditionalLinesCount(1)
    settings.setUseSoftWraps(false)
    settings.setSoftMargins(emptyList[Integer])
  }
}
