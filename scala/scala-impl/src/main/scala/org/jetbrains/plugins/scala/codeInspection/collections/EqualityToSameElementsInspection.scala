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
class EqualityToSameElementsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.equals.on.arrays.and.iterators"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.comparing"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(ArrayEquality, IteratorsEquality)
}

object ArrayEquality extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.equals.with.sameElements")
  override def description: String = ScalaInspectionBundle.message("config.description.for.arrays")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case left `==` right if arraysOrSeqAndArray(left, right) =>
      Some(replace(expr).withText(invocationText(left, "sameElements", right)).highlightRef)
    case left `!=` right if arraysOrSeqAndArray(left, right) =>
      Some(replace(expr).withText(invocationText(negation = true, left, "sameElements", right)).highlightRef)
    case _ => None
  }


  private def arraysOrSeqAndArray(left: ScExpression, right: ScExpression) = {
    isArray(left) && (isArray(right) || isSeq(right)) ||
            isArray(right) && isSeq(left)
  }
}

object IteratorsEquality extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.equals.with.sameElements")
  override def description: String = ScalaInspectionBundle.message("config.description.for.iterators")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case left `==` right if iterators(left, right) =>
      Some(replace(expr).withText(invocationText(left, "sameElements", right)).highlightRef)
    case left `!=` right if iterators(left, right) =>
      Some(replace(expr).withText(invocationText(negation = true, left, "sameElements", right)).highlightRef)
    case _ => None
  }

  private def iterators(left: ScExpression, right: ScExpression) = isIterator(left) && isIterator(right)
}
