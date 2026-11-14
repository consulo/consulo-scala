package org.jetbrains.plugins.scala.compiler.highlighting.services.requests

import consulo.document.Document
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.compilers.DocumentCompiler
import org.jetbrains.plugins.scala.compiler.highlighting.core.FileCompilationScope
import org.jetbrains.plugins.scala.compiler.highlighting.services.util.CompilationUtils
import org.jetbrains.plugins.scala.project.ModuleExt

case class DocumentRequest(
  scope: FileCompilationScope,
  protected val project: Project
) extends CompilationRequest:

  override def originFiles: Map[VirtualFile, Document] = Map(scope.virtualFile -> scope.document)

  override private[services] def execute(): Unit =
    val FileCompilationScope(virtualFile, module, sourceScope, document, _) = scope
    CompilationUtils.performCompilation(project, Set(virtualFile), reportedDocumentVersions) { client =>
      DocumentCompiler.get(project).compile(
        module.findRepresentativeModuleForSharedSourceModuleOrSelf,
        sourceScope,
        document,
        virtualFile,
        client
      )
    }
