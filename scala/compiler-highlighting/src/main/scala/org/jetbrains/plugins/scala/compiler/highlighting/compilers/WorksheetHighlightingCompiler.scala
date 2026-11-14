package org.jetbrains.plugins.scala.compiler.highlighting.compilers

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.document.Document
import consulo.module.Module
import org.jetbrains.jps.incremental.scala.shared.Client
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait WorksheetHighlightingCompiler {

  def compile(
    psiFile: ScalaFile,
    document: Document,
    module: Module,
    client: Client,
  ): Unit
}

object WorksheetHighlightingCompiler
  extends ExtensionPointDeclaration[WorksheetHighlightingCompiler]("org.intellij.scala.worksheetHighlightingCompiler")
    with WorksheetHighlightingCompiler {

  override def compile(
    psiFile: ScalaFile,
    document: Document,
    module: Module,
    client: Client,
  ): Unit =
    implementations.foreach(_.compile(psiFile, document, module, client))
}
