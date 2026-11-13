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
class SortedMaxMinInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.sorted.and.head.last.to.max.min"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "SortedHeadLast"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(SortedHeadIsMin, SortedLastIsMax, SortByHeadIsMinBy, SortByLastIsMaxBy)
}

object SortedHeadIsMin extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.sorted.head.with.min")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.head`(`.sorted`(qual)) =>
      Some(replace(expr).withText(invocationText(qual, "min")).highlightFrom(qual))
    case _ => None
  }
}

object SortedLastIsMax extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.sorted.last.with.max")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.last`(`.sorted`(qual)) =>
      Some(replace(expr).withText(invocationText(qual, "max")).highlightFrom(qual))
    case _ => None
  }
}

object SortByHeadIsMinBy extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.sortBy.head.with.minBy")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.head`(`.sortBy`(qual, f)) =>
      Some(replace(expr).withText(invocationText(qual, "minBy", f)).highlightFrom(qual))
    case _ => None
  }
}

object SortByLastIsMaxBy extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.sortBy.last.with.maxBy")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.last`(`.sortBy`(qual, f)) =>
      Some(replace(expr).withText(invocationText(qual, "maxBy", f)).highlightFrom(qual))
    case _ => None
  }
}
