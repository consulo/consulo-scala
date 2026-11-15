package org.jetbrains.plugins.scala.project.template

import consulo.application.{Application, ApplicationPropertiesComponent}
import consulo.document.{Document, FileDocumentManager}
import consulo.fileEditor.FileEditorManager
import consulo.fileTemplate.FileTemplateManager
import consulo.localize.LocalizeValue
import consulo.platform.Platform
import consulo.project.Project
import consulo.project.startup.StartupManager
import consulo.ui.CheckBox
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.action.IdeActions
import consulo.ui.ex.keymap.util.KeymapUtil
import consulo.virtualFileSystem.LocalFileSystem
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.actions.ShowTypeInfoAction

import java.nio.file.{Files, Path}
import java.util.function.Supplier
import scala.jdk.CollectionConverters.MapHasAsJava

/**
 * "Add sample code" of the new project wizards: the sample files (with onboarding tips as plain text) generated from the
 * internal file templates and opened in the editor when the project is opened.
 */
object ScalaSampleCode {
  private final val AddSampleCodePropertyName = "NewProjectWizard.addSampleCodeState"

  @RequiredUIAccess
  def createCheckBox(): CheckBox =
    CheckBox.create(
      LocalizeValue.of(ScalaBundle.message("new.project.add.sample.code")),
      ApplicationPropertiesComponent.getInstance.getBoolean(AddSampleCodePropertyName, false)
    )

  @RequiredUIAccess
  def isSelected(checkBox: CheckBox): Boolean = {
    val selected = checkBox.getValueOrError.booleanValue
    ApplicationPropertiesComponent.getInstance.setValue(AddSampleCodePropertyName, selected, false)
    selected
  }

  /**
   * Writes the sample files into `sourceRoot` (into the directories of `packagePrefix`) and opens the main one when the
   * project is opened.
   */
  def addScalaSampleCode(project: Project,
                         sourceRoot: Path,
                         isScala3: Boolean,
                         useIndentationBasedSyntax: Boolean,
                         packagePrefix: Option[String],
                         withOnboardingTips: Boolean): Seq[Path] = {
    val manager = FileTemplateManager.getInstance(project)
    val variables = (onboardingTipsVariables(withOnboardingTips) ++ packagePrefix.map("PACKAGE_NAME" -> _)).asJava

    val directory = packagePrefix.fold(sourceRoot)(prefix => sourceRoot.resolve(prefix.replace('.', '/')))
    Files.createDirectories(directory)

    val files = templatesAndFiles(isScala3, isScala3 && useIndentationBasedSyntax, withOnboardingTips).map { case Sample(templateName, fileName) =>
      val file = directory.resolve(fileName)
      Files.writeString(file, manager.getInternalTemplate(templateName).getText(variables))
      file
    }

    openWhenProjectIsOpened(project, files.lastOption)
    files
  }

  /**
   * The file is found and its document is loaded in the background, the editor is opened in the UI thread.
   */
  private def openWhenProjectIsOpened(project: Project, file: Option[Path]): Unit =
    file.foreach { path =>
      StartupManager.getInstance(project).runWhenProjectIsInitialized((() => {
        val uiAccess = project.getUIAccess
        Application.get().executeOnPooledThread((() => {
          Option(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)).foreach { virtualFile =>
            val document = Application.get().runReadAction((() => FileDocumentManager.getInstance.getDocument(virtualFile)): Supplier[Document])
            uiAccess.give((() => {
              if (!project.isDisposed && document != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
              }
            }): Runnable)
          }
        }): Runnable)
      }): Runnable)
    }

  private def templatesAndFiles(isScala3: Boolean, useIndentationBasedSyntax: Boolean, withOnboardingTips: Boolean): Seq[Sample] = {
    val braces = if (useIndentationBasedSyntax) "" else "-braces"
    val tips = if (withOnboardingTips) "-tips" else ""

    if (isScala3) Seq(Sample(s"scala3-main$tips$braces", "main.scala"))
    else Seq(Sample(s"scala2-main$tips", "Main.scala"))
  }

  private def onboardingTipsVariables(withOnboardingTips: Boolean): Map[String, String] =
    if (withOnboardingTips) tipsVariables else Map.empty

  private def shortcut(actionId: String): String = KeymapUtil.getFirstKeyboardShortcutText(actionId)

  private def tipsVariables: Map[String, String] = {
    val isMac = Platform.current.os.isMac
    Map(
      // actions
      "ActionTypeInfo"            -> shortcut(ShowTypeInfoAction.ActionId),
      "ActionImplicitHints"       -> shortcut("Scala.ShowImplicits"),
      "ActionSettings"            -> shortcut(IdeActions.ACTION_SHOW_SETTINGS),
      "ActionRun"                 -> shortcut(IdeActions.ACTION_DEFAULT_RUNNER),
      "ActionShowIntention"       -> shortcut(IdeActions.ACTION_SHOW_INTENTION_ACTIONS),
      "ActionDebug"               -> shortcut(IdeActions.ACTION_DEFAULT_DEBUGGER),
      "ActionSetBreakpoint"       -> shortcut(IdeActions.ACTION_TOGGLE_LINE_BREAKPOINT),
      "ActionSelectInProjectView" -> shortcut("SelectInProjectView"),
      // others
      "META"                      -> "Meta",
      "CONTROL"                   -> "Control",
      "COMMAND"                   -> (if (isMac) "Meta" else "Control"),
      "ProductName"               -> Application.get().getName.get
    )
  }

  private final case class Sample(templateName: String, fileName: String)
}
