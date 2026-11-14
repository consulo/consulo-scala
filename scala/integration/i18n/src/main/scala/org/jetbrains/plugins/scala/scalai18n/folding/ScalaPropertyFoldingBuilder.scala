package org.jetbrains.plugins.scala.scalai18n.folding

import com.intellij.java.language.codeInsight.folding.JavaCodeFoldingSettings
import com.intellij.lang.properties.IProperty
import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.document.Document
import consulo.java.properties.impl.i18n.folding.EditPropertyValueAction
import consulo.language.Language
import consulo.language.ast.ASTNode
import consulo.language.editor.folding.{FoldingBuilderEx, FoldingDescriptor}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScMethodCall, ScReferenceExpression}
import org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n.ScalaI18nUtil.PropertyReferenceResolver

import scala.jdk.CollectionConverters.*

/**
 * Folds `"key"` passed to a `@PropertyKey` parameter (or the whole `Bundle.message("key", args)` call)
 * into the property value, like the Java i18n folding does.
 */
// upstream: done by the platform's UAST based PropertyFoldingBuilder
@ExtensionImpl
final class ScalaPropertyFoldingBuilder extends FoldingBuilderEx {
  import ScalaPropertyFoldingBuilder.*

  @RequiredReadAction
  override def buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array[FoldingDescriptor] = root match {
    case file: ScalaFile if !quick && isFoldingOn =>
      file.depthFirst()
        .collect { case literal: ScStringLiteral => literal }
        .flatMap(foldRegion(_, document))
        .toArray
    case _ => FoldingDescriptor.EMPTY
  }

  override def getPlaceholderText(node: ASTNode): String = node.getText

  override def isCollapsedByDefault(node: ASTNode): Boolean = isFoldingOn

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

object ScalaPropertyFoldingBuilder {
  private val FoldMaxLength = 50

  private def isFoldingOn: Boolean = JavaCodeFoldingSettings.getInstance.isCollapseI18nMessages

  private def foldRegion(literal: ScStringLiteral, document: Document): Option[FoldingDescriptor] = {
    if (literal.getValue == null || literal.getValue.isEmpty) return None

    val resolver = PropertyReferenceResolver(literal)
    if (!resolver.isPassedToPropertyKey) return None

    resolver.referencedProperties.headOption.map { property =>
      val dependencies: java.util.Set[AnyRef] = Set[AnyRef](property).asJava
      val (element, placeholder) = foldedCall(literal, property, resolver)
        .getOrElse(literal -> quoted(property.getValue))
      EditPropertyValueAction.registerFoldedElement(element, document)
      new FoldingDescriptor(element.getNode, element.getTextRange, null, placeholder, null, dependencies)
    }
  }

  // `Bundle.message("key", a, b)` -> `"value with {a} and {b}"`
  private def foldedCall(literal: ScStringLiteral, property: IProperty, resolver: PropertyReferenceResolver): Option[(PsiElement, String)] =
    literal.getParent match {
      case (argList: ScArgumentExprList) & Parent(call: ScMethodCall) if argList.exprs.headOption.contains(literal) =>
        val args = argList.exprs.tail
        val paramCount = resolver.possibleParamCounts.maxOption.getOrElse(0)
        val argTexts = args.map {
          case lit: ScLiteral if lit.getValue != null => Some(lit.getValue.toString)
          case ref: ScReferenceExpression => Some("{" + ref.getText + "}")
          case _ => None
        }
        if (args.length != paramCount || argTexts.contains(None)) None
        else {
          val text = argTexts.flatten.zipWithIndex.foldLeft(property.getValue) {
            case (acc, (arg, idx)) => acc.replace("{" + idx + "}", arg)
          }.replace("''", "'")
          Some(call -> quoted(text))
        }
      case _ => None
    }

  private def quoted(value: String): String = {
    val text = "\"" + value + "\""
    if (text.length > FoldMaxLength) text.substring(0, FoldMaxLength - 3) + "...\""
    else text
  }
}
