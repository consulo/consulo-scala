package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

object ReverseIterator extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.reverse.iterator")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case `.iterator`(`.reverse`(qual)) =>
        Some(replace(expr).withText(invocationText(qual, "reverseIterator")).highlightFrom(qual))
      case _ => None
    }
  }
}

@ExtensionImpl
class ReverseIteratorInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.reverse.and.iterator.to.reverseiterator"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(ReverseIterator)
}
