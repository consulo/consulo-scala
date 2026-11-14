package org.jetbrains.idea.devkit.scala

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.devkit.localize.folding.LocalizeFoldingBuilder
import consulo.document.Document
import consulo.language.Language
import consulo.language.ast.ASTNode
import consulo.language.editor.folding.{FoldingBuilderEx, FoldingDescriptor}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScMethodCall, ScReferenceExpression}

/**
 * Folds `XxxLocalize.someKey()` / `XxxLocalize.someKey(args)` in Scala code into the localized text,
 * like consulo-devkit's LocalizeFoldingBuilder for Java.
 */
@ExtensionImpl
final class ScalaLocalizeFoldingBuilder extends FoldingBuilderEx {
  import ScalaLocalizeFoldingBuilder.*

  @RequiredReadAction
  override def buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array[FoldingDescriptor] = root match {
    case file: ScalaFile if !quick =>
      file.depthFirst()
        .collect { case ref: ScReferenceExpression => ref }
        .flatMap(foldRegion)
        .toArray
    case _ => FoldingDescriptor.EMPTY
  }

  override def getPlaceholderText(node: ASTNode): String = node.getText

  override def isCollapsedByDefault(node: ASTNode): Boolean = true

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

object ScalaLocalizeFoldingBuilder {
  private val LocalizeSuffix = "Localize"

  @RequiredReadAction
  private def foldRegion(ref: ScReferenceExpression): Option[FoldingDescriptor] = ref.qualifier match {
    case Some(qualifier: ScReferenceExpression) if qualifier.refName.endsWith(LocalizeSuffix) =>
      for {
        localizeClass <- qualifier.resolve().toOption
        info <- LocalizeFoldingBuilder.findLocalizeInfo(ref, localizeClass, ref.refName).toOption
      } yield {
        val element = ref.getParent match {
          case call: ScMethodCall if call.getInvokedExpr == ref => call
          case _ => ref
        }
        new FoldingDescriptor(element.getNode, element.getTextRange, null, info.value())
      }
    case _ => None
  }
}
