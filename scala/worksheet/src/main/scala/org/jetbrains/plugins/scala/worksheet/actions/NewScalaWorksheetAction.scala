package org.jetbrains.plugins.scala
package worksheet
package actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.fileTemplate.AttributesDefaults
import consulo.ui.ex.action.{AnActionEvent, AnActionWithSyncUpdate}
import consulo.language.editor.CommonDataKeys
import consulo.dataContext.DataContext
import consulo.module.Module
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.{PsiDirectory, PsiFile}
import org.jetbrains.plugins.scala.actions.LazyFileTemplateAction
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.project._
import org.jetbrains.plugins.scala.worksheet.actions.NewScalaWorksheetAction._

@ActionImpl(id = "Scala.NewScalaWorksheet", parents = Array(new ActionParentRef(value = new ActionRef(id = "NewGroup"), anchor = ActionRefAnchor.BEFORE, relatedToAction = new ActionRef(id = "NewFromTemplate"))))
final class NewScalaWorksheetAction extends LazyFileTemplateAction(
  "Scala Worksheet",
  WorksheetBundle.message("new.scalaworksheet.menu.action.text"),
  WorksheetBundle.message("new.scalaworksheet.menu.action.description"),
  WorksheetFileType.getIcon
) with AnActionWithSyncUpdate {

  override def getAttributesDefaults(dataContext: DataContext): AttributesDefaults = {
    val selectedElement = dataContext.getData(CommonDataKeys.PSI_ELEMENT)
    val attributes = selectedElement match {
      case directory: PsiDirectory =>
        suggestIndex(directory.getVirtualFile).headOption
      case file: PsiFile =>
        val parent = Option(file.getVirtualFile.getParent)
        parent.flatMap(suggestIndex(_).headOption)
      case _ =>
        None
    }
    attributes.orNull
  }

  override def update(event: AnActionEvent): Unit = {
    val isEnabled = event.getDataContext.getData(Module.KEY).toOption.exists(_.hasScala)

    val presentation = event.getPresentation
    presentation.setEnabled(isEnabled)
    presentation.setVisible(isEnabled)
    presentation.setIcon(icon)
  }

}

object NewScalaWorksheetAction {

  private def suggestIndex(file: VirtualFile) = for {
    index <- "" #:: LazyList.tabulate(1000)(index => (index + 1).toString)
    defaultName = "untitled" + index

    if file.findChild(defaultName + "." + WorksheetFileType.getDefaultExtension) == null
  } yield new AttributesDefaults(defaultName)
}
