package org.jetbrains.plugins.scala
package worksheet
package actions

import consulo.annotation.component.ActionImpl
import consulo.fileEditor.FileEditorManager
import consulo.language.editor.CommonDataKeys
import consulo.language.scratch.{RootType, ScratchFileService}
import consulo.logging.Logger
import consulo.project.Project
import consulo.ui.ex.action.{AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.util.lang.StringUtil.notNullize
import consulo.virtualFileSystem.VirtualFile
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.plugins.scala.actions.ScalaActionUtil.enableAndShowIfInScalaFile
import org.jetbrains.plugins.scala.extensions.executeWriteActionCommand
import org.jetbrains.plugins.scala.worksheet.ScalaScratchFileCreationHelper.worksheetScratchFileType

import java.io.IOException

@ActionImpl(id = "Scala.CreateLightWorksheetAction")
final class CreateLightWorksheetAction extends AnAction(
  WorksheetBundle.message("create.light.scala.worksheet.menu.action.text"),
  WorksheetBundle.message("create.light.scala.worksheet.menu.action.description"),
  /*icon = */ null
) with AnActionWithSyncUpdate {

  override def actionPerformed(event: AnActionEvent): Unit = {
    val project = event.getData(Project.KEY)
    if (project == null) return
    val text = event.getData(CommonDataKeys.EDITOR) match {
      case null => null
      case editor => editor.getSelectionModel.getSelectedText
    }

    val fileType = worksheetScratchFileType
    val file = createScratchFile(project, s"scratch.${fileType.getDefaultExtension}", notNullize(text))
    if (file != null) {
      FileEditorManager.getInstance(project).openFile(file, true)
    }
  }

  // upstream: ScratchRootType.getInstance.createScratchFile (internal in Consulo)
  private def createScratchFile(project: Project, fileName: String, text: String): VirtualFile = {
    val fileType = worksheetScratchFileType
    var result: VirtualFile = null
    executeWriteActionCommand(WorksheetBundle.message("create.light.scala.worksheet.menu.action.text")) {
      try {
        val fileService = ScratchFileService.getInstance
        val file = fileService.findFile(RootType.findById("scratches"), fileName, ScratchFileService.Option.create_new_always)
        fileService.getScratchesMapping.setMapping(file, fileType.getLanguage)
        VirtualFileUtil.saveText(file, text)
        result = file
      } catch {
        case e: IOException =>
          CreateLightWorksheetAction.Log.warn(e)
      }
    }(using project)
    result
  }

  override def update(event: AnActionEvent): Unit =
    enableAndShowIfInScalaFile(event)
}

object CreateLightWorksheetAction {
  private val Log = Logger.getInstance(classOf[CreateLightWorksheetAction])
}
