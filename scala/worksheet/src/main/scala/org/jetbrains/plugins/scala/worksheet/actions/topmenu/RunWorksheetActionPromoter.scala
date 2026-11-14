package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.annotation.component.ExtensionImpl

import consulo.ui.ex.action.AnAction
import consulo.dataContext.DataContext
import consulo.fileEditor.FileEditor
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.actions.SingleActionPromoterBase

@ExtensionImpl
class RunWorksheetActionPromoter extends SingleActionPromoterBase {

  override def shouldPromote(anAction: AnAction, context: DataContext): Boolean =
    anAction.isInstanceOf[RunWorksheetAction] && isInInScalaFile(context)

  private def isInInScalaFile(context: DataContext): Boolean = {
    //According to Greg Shrago context.getData(CommonDataKeys.VIRTUAL_FILE) might be not reliable approach
    //and better to use file editor instead
    val fileEditor = context.getData(FileEditor.KEY)
    fileEditor != null && {
      val vFile = fileEditor.getFile
      vFile != null && hasScalaWorksheetExtension(vFile)
    }
  }

  private def hasScalaWorksheetExtension(vFile: VirtualFile) = {
    val extension = vFile.getExtension
    extension == "sc" ||
      extension == "scala" // for Scala Scratch files which behave like extensions
  }
}