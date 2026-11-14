package org.jetbrains.plugins.scala.compiler.highlighting.core

import consulo.document.Document
import consulo.module.Module
import consulo.project.Project
import consulo.project.content.TestSourcesFilter
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiFile
import org.jetbrains.jps.incremental.scala.shared.remote.SourceScope

final case class FileCompilationScope(
  virtualFile: VirtualFile,
  module: Module,
  sourceScope: SourceScope,
  document: Document,
  psiFile: PsiFile
)

object FileCompilationScope:

  /** The source scope `virtualFile` belongs to in `project`. */
  def sourceScopeOf(project: Project, virtualFile: VirtualFile): SourceScope =
    if (TestSourcesFilter.isTestSources(virtualFile, project)) SourceScope.Test
    else SourceScope.Production