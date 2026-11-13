package org.jetbrains.plugins.scala.project

import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiManager
import org.jetbrains.plugins.scala.api.ScalaLanguage

package object notification {

  // upstream: org.jetbrains.sbt.Sbt.Extension
  private val SbtExtension = ".sbt"

  private[jetbrains]
  def isScalaSourceFile(file: VirtualFile, project: Project): Boolean =
    if (file.isWritable) {
      val psiFile = PsiManager.getInstance(project).findFile(file)
      if (psiFile != null) {
        psiFile.getLanguage.isKindOf(ScalaLanguage.INSTANCE) &&
          !psiFile.getName.endsWith(SbtExtension) // root sbt files belong to main (not *-build) modules
      }
      else false
    }
    else false
}
