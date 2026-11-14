package org.jetbrains.scalaCli.script

import consulo.annotation.component.ExtensionImpl

import consulo.module.content.ProjectRootManager
import consulo.language.psi.resolve.PsiScopeProcessor
import consulo.language.psi.PsiElement
import consulo.language.psi.resolve.ResolveState
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.lang.psi.api.{FileDeclarationsContributor, ScalaFile}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

/**
 * `*.sc` files which are run by Scala CLI have an implicit `args` top level definition
 *
 * @see https://scala-cli.virtuslab.org/docs/getting_started#scripting
 * @see https://scala-cli.virtuslab.org/docs/commands/run#passing-arguments
 */
@ExtensionImpl
final class ScalaCliScriptFileDeclarationsContributor extends FileDeclarationsContributor {

  override def accept(holder: PsiElement): Boolean =
    holder match {
      case file: ScalaFile =>
        //checking the extension instead of checking for WorksheetFile
        // just not to add an extra module dependency, which is not that necessary for now
        file.getViewProvider.getVirtualFile.getExtension == "sc"
      case _ =>
        false
    }

  override def muteUnresolvedSymbolInCompilerBasedHighlighting(symbolName: String): Boolean =
    symbolName == "args"

  override def processAdditionalDeclarations(
    processor: PsiScopeProcessor,
    holder: PsiElement,
    state: ResolveState,
    lastParent: PsiElement
  ): Unit = {
    val argsDef = cachedInUserData("scalaCliScriptInjectedSyntheticElements", holder, ProjectRootManager.getInstance(holder.getProject)) {
      ScalaPsiElementFactory.createDefinitionWithContext("def args: Array[String] = ???", holder, null)
    }

    processor.execute(argsDef, state)
  }
}
