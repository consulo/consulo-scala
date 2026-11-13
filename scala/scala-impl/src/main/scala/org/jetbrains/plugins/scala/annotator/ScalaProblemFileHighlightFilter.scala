package org.jetbrains.plugins.scala.annotator

import consulo.annotation.component.ExtensionImpl

import consulo.compiler.CompilerManager
import consulo.language.file.FileTypeManager
import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.util.lang.function.Condition
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiManager
import org.jetbrains.plugins.scala.annotator.ScalaProblemFileHighlightFilter._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.settings.{ScalaHighlightingMode, ScalaProjectSettings}

@ExtensionImpl
class ScalaProblemFileHighlightFilter(project: Project) extends Condition[VirtualFile] {
  /**
    * @see [[com.intellij.codeInsight.problems.DefaultProblemFileHighlightFilter]]
    */
  override def value(file: VirtualFile): Boolean = PsiManager.getInstance(project).findFile(file) match {
    case sf: ScalaFile =>
      (isEnabled || ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(sf)) &&
        isScalaSourceFile(project, file) &&
        !CompilerManager.getInstance(project).isExcludedFromCompilation(file.toNioPath)
    case _ => false
  }

  private def isEnabled: Boolean =
    ScalaProjectSettings.getInstance(project).isProjectViewHighlighting
}

private object ScalaProblemFileHighlightFilter {
  /**
    * @see [[com.intellij.openapi.roots.FileIndexUtil]]
    */
  private def isScalaSourceFile(project: Project, file: VirtualFile): Boolean = {
    if (!file.isDirectory && !FileTypeManager.getInstance.isFileIgnored(file)) {
      val fileIndex = ProjectRootManager.getInstance(project).getFileIndex
      fileIndex.isInSourceContent(file) || fileIndex.isInLibrarySource(file)
    } else {
      false
    }
  }
}
