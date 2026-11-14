package org.jetbrains.plugins.scala.compiler.highlighting.triggers

import consulo.application.PowerSaveMode
import consulo.document.Document
import consulo.module.Module
import consulo.project.Project
import consulo.java.impl.util.JavaProjectRootsUtil
import consulo.module.content.ProjectRootManager
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiFile
import com.intellij.java.language.psi.PsiJavaFile
import org.jetbrains.plugins.scala.compiler.highlighting.core.FileCompilationScope
import org.jetbrains.plugins.scala.compiler.highlighting.core.FileCompilationScope.sourceScopeOf
import org.jetbrains.plugins.scala.extensions.{PsiFileExt, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

private[highlighting] object TriggerUtil:

  // upstream: also requires the compile server to be enabled, here the compilation runs in the IDE process
  def isHighlightingEnabled: Boolean =
    !PowerSaveMode.isEnabled

  def isHighlightingEnabled(project: Project): Boolean =
    isHighlightingEnabled && ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project)

  def isHighlightingEnabledFor(psiFile: PsiFile, virtualFile: VirtualFile, project: Project): Boolean = inReadAction {
    ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(psiFile) &&
      virtualFile.isInLocalFileSystem &&
      (psiFile match {
        case _ if psiFile.isScalaWorksheet => true
        case _: ScalaFile | _: PsiJavaFile if !JavaProjectRootsUtil.isOutsideSourceRoot(psiFile) => true
        case _ => false
      }) &&
      ScalaHighlightingMode.shouldHighlightBasedOnFileLevel(psiFile, project)
  }

  def moduleFor(project: Project, virtualFile: VirtualFile): Module =
    inReadAction(ProjectRootManager.getInstance(project).getFileIndex.getModuleForFile(virtualFile))

  def fileCompilationScope(project: Project, virtualFile: VirtualFile, module: Module,
                                   document: Document, psiFile: PsiFile): FileCompilationScope =
    FileCompilationScope(virtualFile, module, sourceScopeOf(project, virtualFile), document, psiFile)
