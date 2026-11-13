package org.jetbrains.plugins.scala.util

import consulo.module.Module
import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile
import org.jetbrains.plugins.scala.api.project._
import org.jetbrains.plugins.scala.rt.project._
import org.jetbrains.plugins.scala.shared.project._

object ScalaUtil {

  def getScalaVersion(file: PsiFile): Option[String] = file match {
    case ScFile.VirtualFile(virtualFile) =>
      getModuleForFile(virtualFile)(using file.getProject)
        .flatMap(_.scalaSdk)
        .flatMap(_.libraryVersion)
    case _ => None
  }

  def getModuleForFile(file: VirtualFile)
                      (implicit project: Project): Option[Module] = inReadAction {
    Option(ProjectRootManager.getInstance(project).getFileIndex.getModuleForFile(file))
  }
}