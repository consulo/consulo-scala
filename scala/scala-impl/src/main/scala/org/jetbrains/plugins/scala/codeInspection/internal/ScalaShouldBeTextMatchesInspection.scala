package org.jetbrains.plugins.scala.codeInspection.internal

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.collections.{OperationOnCollectionInspection, Qualified, Simplification, SimplificationType, `!=`, `==`, invocation, invocationText}
import org.jetbrains.plugins.scala.codeInspection.{ExpressionOfTypeMatcher, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class ScalaShouldBeTextMatchesInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("internal.getText.equals.shouldBe.textMatches"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.internal"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(ScalaShouldBeTextMatchesInspection)
}

object ScalaShouldBeTextMatchesInspection extends SimplificationType() {
  override val hint: String = ScalaInspectionBundle.message("internal.replace.with.textMatches")

  private val `.getText`: Qualified = invocation("getText").from(ArraySeq(psiElementFqn))

  private val stringExpr = new ExpressionOfTypeMatcher("java.lang.String")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.getText`(base) `==` (stringExpr(str)) =>
      Some(replace(expr).withText(invocationText(base, "textMatches", str)).highlightFrom(base))
    case stringExpr(str) `==` (`.getText`(base)) =>
      Some(replace(expr).withText(invocationText(base, "textMatches", str)).highlightAll)
    case `.getText`(base) `!=` (stringExpr(str)) =>
      Some(replace(expr).withText(invocationText(negation = true, base, "textMatches", str)).highlightFrom(base))
    case stringExpr(str) `!=` (`.getText`(base)) =>
      Some(replace(expr).withText(invocationText(negation = true, base, "textMatches", str)).highlightAll)
    case _ =>
      None
  }
}
