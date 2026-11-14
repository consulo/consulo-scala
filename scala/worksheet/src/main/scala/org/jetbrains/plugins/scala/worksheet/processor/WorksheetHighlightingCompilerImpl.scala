package org.jetbrains.plugins.scala.worksheet.processor

import consulo.annotation.component.ExtensionImpl
import consulo.document.Document
import consulo.module.Module
import org.jetbrains.jps.incremental.scala.shared.Client
import org.jetbrains.plugins.scala.compiler.highlighting.compilers.WorksheetHighlightingCompiler
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
final class WorksheetHighlightingCompilerImpl
  extends WorksheetHighlightingCompiler {

  override def compile(
    psiFile: ScalaFile,
    document: Document,
    module: Module,
    client: Client,
  ): Unit = {
    assert(psiFile.isWorksheetFile, "WorksheetHighlightingCompilerImpl must be called for worksheet files only")

    if (module.isDisposed)
      return

    val compiler = new WorksheetCompiler(module, psiFile)
    // upstream: waits at most 60 seconds, the in-process compilation is synchronous
    compiler.compileOnlySync(
      document,
      client
    )
  }
}
