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
class ZeroIndexToHeadInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.access.to.first.element.by.index"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.indices"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(ZeroIndexToHead)
}

object ZeroIndexToHead extends SimplificationType() {
  override def hint: String = ScalaInspectionBundle.message("replace.with.head")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.apply`(literal("0")) if isSeq(qual) && !isIndexedSeq(qual) =>
        Some(replace(expr).withText(invocationText(qual, "head")).highlightFrom(qual))
      case _ => None
    }
  }
}
