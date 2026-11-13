package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class FilterEmptyCheckInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.filter.and.emptiness.check.to.exists.forall"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(FilterIsEmptyCheck, FilterNonEmptyCheck, FilterNotIsEmptyCheck, FilterNotNonEmptyCheck)
}

object FilterIsEmptyCheck extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("filter.empty.check.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case CheckIsEmpty(qual`.filter`(pred), s, e) if !hasSideEffects(pred) =>
        val notExistsText = invocationText(negation = true, qual, "exists", pred)
        val start = Math.min(s, qual.end)
        val end = Math.max(e, expr.end)
        Some(replace(expr).withText(notExistsText).highlightRange(start, end))
      case _ => None
    }
  }
}

object FilterNonEmptyCheck extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("filter.nonempty.check.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case CheckNonEmpty(qual`.filter`(pred), s, e) if !hasSideEffects(pred) =>
      val existsText = invocationText(qual, "exists", pred)
      val start = Math.min(s, qual.end)
      val end = Math.max(e, expr.end)
      Some(replace(expr).withText(existsText).highlightRange(start, end))
    case _ => None
  }
}

object FilterNotIsEmptyCheck extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("filterNot.empty.check.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case CheckIsEmpty(qual`.filterNot`(pred), s, e) =>
      val start = Math.min(s, qual.end)
      val end = Math.max(e, expr.end)
      val forallText = invocationText(qual, "forall", pred)
      Some(replace(expr).withText(forallText).highlightRange(start, end))
    case _ => None
  }
}

object FilterNotNonEmptyCheck extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("filterNot.nonempty.check.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case CheckNonEmpty(qual`.filterNot`(pred), s, e) =>
      val start = Math.min(s, qual.end)
      val end = Math.max(e, expr.end)
      val notForallText = invocationText(negation = true, qual, "forall", pred)
      Some(replace(expr).withText(notForallText).highlightRange(start, end))
    case _ => None
  }
}