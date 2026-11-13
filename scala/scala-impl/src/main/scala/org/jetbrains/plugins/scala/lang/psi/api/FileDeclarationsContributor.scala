package org.jetbrains.plugins.scala.lang.psi.api

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.psi.resolve.PsiScopeProcessor
import consulo.language.psi.PsiElement
import consulo.language.psi.resolve.ResolveState
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

@ExtensionAPI(ComponentScope.APPLICATION)
abstract class FileDeclarationsContributor {
  def accept(holder: PsiElement): Boolean

  def processAdditionalDeclarations(processor: PsiScopeProcessor, holder: PsiElement, state: ResolveState, lastParent: PsiElement): Unit

  def muteUnresolvedSymbolInCompilerBasedHighlighting(symbolName: String): Boolean = false
}

object FileDeclarationsContributor
  extends ExtensionPointDeclaration[FileDeclarationsContributor](
    "org.intellij.scala.fileDeclarationsContributor"
  ) {
  
  def getAllFor(holder: PsiElement): Seq[FileDeclarationsContributor] =
    implementations.filter(_.accept(holder))
}
