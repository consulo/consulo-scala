package org.jetbrains.plugins.scala.worksheet.actions

import consulo.module.Module
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.project.{ModuleExt, UserDataKeys}
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings

import scala.ref.WeakReference

private object WorksheetModuleUtil {

  def ensureModuleAttached(project: Project, virtualFile: VirtualFile): Unit = {
    for {
      wrapperModule <- moduleForFile(project, virtualFile)
    } {
      val moduleReferenceRef = WeakReference(wrapperModule)
      virtualFile.putUserData(UserDataKeys.SCALA_ATTACHED_MODULE, moduleReferenceRef)
    }
  }

  private def moduleForFile(project: Project, virtualFile: VirtualFile): Option[Module] = {
    //noinspection ApiStatus
    val cpModule = WorksheetFileSettings(project, virtualFile).getModule
    cpModule.map(_.findRepresentativeModuleForSharedSourceModuleOrSelf)
  }
}
