package org.jetbrains.plugins.scala.lang.references

import consulo.document.util.TextRange
import com.intellij.java.language.patterns.PsiJavaPatterns.psiJavaElement
import consulo.language.Language
import consulo.language.psi.{PsiReferenceProviderByType, ReferenceProviderType}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import consulo.language.pattern.PlatformPatterns
import com.intellij.java.language.patterns.PsiJavaElementPattern
import consulo.language.psi.{PsiElement, PsiReference, PsiReferenceContributor, PsiReferenceProvider, PsiReferenceRegistrar}
import com.intellij.java.impl.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import consulo.language.ast.TokenSet
import consulo.language.util.{IncorrectOperationException, ProcessingContext}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScInterpolationPattern
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScInterpolated, ScInterpolatedStringLiteral, ScLiteral}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionWithContextFromText
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScPsiDocToken

import scala.collection.mutable.ListBuffer

/**
 * @see [[com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaReferenceContributor]]
 */
final class ScalaReferenceContributor extends PsiReferenceContributor {

  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit = {
    def literalCapture: PsiJavaElementPattern.Capture[ScStringLiteral] = psiJavaElement(classOf[ScStringLiteral])

    registrar.registerReferenceProvider(literalCapture, new ScalaFilePathReferenceProvider(), PsiReferenceRegistrar.LOWER_PRIORITY)
    registrar.registerReferenceProvider(literalCapture, new InterpolatedStringReferenceProvider())
    // upstream: ArbitraryPlaceUrlReferenceProvider and CommentsReferenceContributor.COMMENTS_REFERENCE_PROVIDER_TYPE;
    // in Consulo the url reference provider is the provider of the comments type
    val commentsProvider = PsiReferenceProviderByType.forType(ReferenceProviderType.COMMENTS)
    if (commentsProvider != null) {
      registrar.registerReferenceProvider(literalCapture, commentsProvider)

      registrar.registerReferenceProvider(
        PlatformPatterns.psiElement(classOf[ScPsiDocToken]),
        commentsProvider
      )
    }
  }

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

private final class InterpolatedStringReferenceProvider extends PsiReferenceProvider {

  import PsiReference.EMPTY_ARRAY

  override def getReferencesByElement(element: PsiElement, context: ProcessingContext): Array[PsiReference] = element match {
    case _: ScInterpolatedStringLiteral => EMPTY_ARRAY
    case literal: ScStringLiteral if literal.hasValidClosingQuotes && literal.textContains('$') => // TODO remove this hack
      try {
        createExpressionWithContextFromText("s" + literal.getText, literal.getContext) match {
          case interpolated: ScInterpolatedStringLiteral =>
            for {
              child <- interpolated.getInjections.toArray
              if child.is[ScReferenceExpression]
            } yield new InterpolatedStringPsiReference(
              child.asInstanceOf[ScReferenceExpression],
              literal,
              interpolated
            )
          case _ => EMPTY_ARRAY
        }
      } catch {
        case _: IncorrectOperationException => EMPTY_ARRAY
      }
    case _ => EMPTY_ARRAY
  }
}

private class InterpolatedStringPsiReference(ref: ScReferenceExpression, literal: ScLiteral, interpolated: ScExpression) extends PsiReference {
  override def getCanonicalText: String = ref.getCanonicalText

  override def getElement: PsiElement = literal

  override def isReferenceTo(element: PsiElement): Boolean = ref.isReferenceTo(element)

  override def bindToElement(element: PsiElement): PsiElement = ref

  override def handleElementRename(newElementName: String): PsiElement = ref

  override def isSoft: Boolean = true

  override def getRangeInElement: TextRange = {
    val startOffset = interpolated.getTextRange.getStartOffset + 1
    ref.getTextRange.shiftLeft(startOffset)
  }

  override def resolve(): PsiElement = null
}

private class ScalaFilePathReferenceProvider() extends FilePathReferenceProvider {
  import ScalaFilePathReferenceProvider._

  override def getReferencesByElement(element: PsiElement, context: ProcessingContext): Array[PsiReference] =
    element match {
      case interp: ScInterpolated => getReferencesForInterpolated(interp)
      case ScStringLiteral(text)  => getReferencesByElement(element, text, 1, true)
      case _                      => PsiReference.EMPTY_ARRAY
    }

  private def getReferencesForInterpolated(interpolated: ScInterpolated): Array[PsiReference] = {
    val parts = getStringParts(interpolated)
    val start = interpolated.startOffset
    parts.flatMap { element =>
      val offset = element.startOffset - start
      getReferencesByElement(interpolated, element.getText, offset, true)
    }
  }

  // do not replace with interpolated.getStringParts
  // comment from revision f4f57ef:
  // these references have nothing to do with ScInterpolatedExpressionPrefix
  private def getStringParts(interpolated: ScInterpolated): Array[PsiElement] = {
    val res = ListBuffer[PsiElement]()
    val children: Array[PsiElement] = interpolated match {
      case ip: ScInterpolationPattern => ip.args.children.toArray
      case sl: ScInterpolatedStringLiteral => Option(sl.getFirstChild.getNextSibling).toArray
    }
    for (child <- children) {
      if (acceptedInterpolatedTokens.contains(child.getNode.getElementType))
        res += child
    }
    res.toArray
  }
}

private object ScalaFilePathReferenceProvider {

  private val acceptedInterpolatedTokens = TokenSet.create(
    ScalaTokenTypes.tINTERPOLATED_STRING,
    ScalaTokenTypes.tINTERPOLATED_MULTILINE_STRING
  )
}
