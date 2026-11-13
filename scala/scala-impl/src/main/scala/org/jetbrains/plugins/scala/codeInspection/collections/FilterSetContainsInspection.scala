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
final class FilterSetContainsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.contains.in.filter"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(FilterSetContainsInspection)
}

object FilterSetContainsInspection extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("remove.redundant.contains")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      // TODO infix notation?
      case qual `.filter` `.contains`(set) if isSet(set) =>
        val highlightStart = set.end + 1
        val highlightEnd = expr.end - 1
        Some(replace(expr).withText(invocationText(qual, "filter", set)).highlightRange(highlightStart, highlightEnd))
      case _ => None
    }
  }
}
