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
class FilterSizeInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.filter.and.size.to.count"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(FilterSize)
}

object FilterSize extends SimplificationType {

  override def hint: String = ScalaInspectionBundle.message("filter.size.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] =
    expr match {
      // TODO infix notation?
      case `.sizeOrLength`(qual`.filter`(cond)) =>
        Some(replace(expr).withText(invocationText(qual, "count", cond)).highlightFrom(qual))
      case _ =>
        None
    }
}
