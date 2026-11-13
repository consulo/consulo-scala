package org.jetbrains.plugins.scala.actions

import consulo.ui.ex.awt.UIExAWTDataKey
import consulo.module.Module
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.module.content.ModuleRootManager
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.plugins.scala.{ScalaBundle, extensions}

import javax.swing.JComponent

class EditPackagePrefixAction extends AnAction(ScalaBundle.message("edit.package.prefix")) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val modifiableModel = {
      val module = e.getDataContext.getData(Module.KEY)
      ModuleRootManager.getInstance(module).getModifiableModel
    }

    val dialog = {
      val file = e.getDataContext.getData(CommonDataKeys.VIRTUAL_FILE)
      val sourceFolder = modifiableModel.getContentEntries.flatMap(_.getSourceFolders).find(_.getFile == file)
      val properties = sourceFolder.get.getJpsElement.getProperties(JavaModuleSourceRootTypes.SOURCES)
      new SourceRootPropertiesDialog(e.getDataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT).asInstanceOf[JComponent], properties)
    }

    if (dialog.showAndGet) {
      extensions.inWriteAction {
        modifiableModel.commit()
      }
    }
  }

  override def update(e: AnActionEvent): Unit = {
    super.update(e)

    val isSourceRoot =
      Option(e.getDataContext.getData(Module.KEY))
        .flatMap(module => Option(e.getDataContext.getData(CommonDataKeys.VIRTUAL_FILE))
          .map(file => ModuleRootManager.getInstance(module).getSourceRoots.contains(file)))
        .getOrElse(false)

    e.getPresentation.setEnabledAndVisible(isSourceRoot)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
