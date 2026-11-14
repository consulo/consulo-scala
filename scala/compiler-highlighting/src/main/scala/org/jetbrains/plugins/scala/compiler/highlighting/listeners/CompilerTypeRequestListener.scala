package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import consulo.module.content.ProjectFileIndex
import consulo.project.Project
import jakarta.inject.Inject
import org.jetbrains.jps.incremental.scala.shared.remote.SourceScope
import org.jetbrains.plugins.scala.compiler.highlighting.compilers.DocumentCompiler
import org.jetbrains.plugins.scala.compiler.highlighting.core.FileCompilationScope
import org.jetbrains.plugins.scala.compiler.highlighting.services.HighlightingService
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.DocumentRequest
import org.jetbrains.plugins.scala.lang.psi.impl.CompilerType
import org.jetbrains.plugins.scala.project.ProjectPsiFileExt

@TopicImpl(ComponentScope.PROJECT)
final class CompilerTypeRequestListener @Inject()(project: Project) extends CompilerType.Listener {

  def onCompilerTypeRequest(e: PsiElement): Unit = {
    if (project.isDisposed) return

    val psiFile = e.getContainingFile
    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
    val module = psiFile.module.getOrElse(throw new IllegalArgumentException("The containing file must belong to a module"))
    // Consulo: there are no compiler types without the compiler plugin, see DocumentCompiler.compilerPluginJar
    if (DocumentCompiler.compilerPluginJar(module).isEmpty) return

    val virtualFile = psiFile.getVirtualFile
    val scope = if (ProjectFileIndex.getInstance(project).isInSource(virtualFile)) SourceScope.Production else SourceScope.Test
    val request = DocumentRequest(FileCompilationScope(virtualFile, module, scope, document, psiFile), project)
    // Compiled right away rather than scheduled: the caller is waiting for the compiler-provided type.
    HighlightingService(project).compile(request)
  }
}
