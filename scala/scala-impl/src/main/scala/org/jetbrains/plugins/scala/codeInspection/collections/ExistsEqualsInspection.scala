package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScMethodCall, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt}
import org.jetbrains.plugins.scala.lang.psi.types.result._

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class ExistsEqualsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.exists.simplifiable.to.contains"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(ExistsEquals, ForallNotEquals)
}

//noinspection ScalaUnnecessaryParentheses
object ExistsEquals extends SimplificationType {
  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.exists`(`x == `(e)) if canBeReplacedWithContains(qual, e) =>
        Some(replace(expr).withText(invocationText(qual, "contains", e)).highlightFrom(qual))
      case _ => None
    }
  }

  override def hint: String = ScalaInspectionBundle.message("exists.equals.hint")

  def canBeReplacedWithContains(qual: ScExpression, arg: ScExpression): Boolean = {
    implicit val context: Context = Context(qual)

    if (qual == null) return false

    val exprText = s"(${qual.getText}).contains(${arg.getText})"
    ScalaPsiElementFactory.createExpressionWithContextFromText(exprText, qual.getContext, qual) match {
      case ScMethodCall(ref: ScReferenceExpression, Seq(a)) =>
        ref.resolve() != null &&
          a.expectedType(fromUnderscore = false).exists(a.`type`().getOrNothing.conforms(_))
      case _ => false
    }

  }
}

//noinspection ScalaUnnecessaryParentheses
object ForallNotEquals extends SimplificationType {
  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.forall`(`x != `(e)) if ExistsEquals.canBeReplacedWithContains(qual, e) =>
        Some(replace(expr).withText("!" + invocationText(qual, "contains", e)).highlightFrom(qual))
      case _ => None
    }
  }

  override def hint: String = ScalaInspectionBundle.message("forall.notEquals.hint")
}
