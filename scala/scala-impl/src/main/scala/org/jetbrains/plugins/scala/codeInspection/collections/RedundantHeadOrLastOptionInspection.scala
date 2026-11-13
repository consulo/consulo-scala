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
class RedundantHeadOrLastOptionInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.headoption.or.lastoption"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.options"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(RedundantHeadOption, RedundantLastOption)
}

object RedundantHeadOption extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("remove.redundant.headOption")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.headOption`(qual) if isOption(qual) =>
      Some(replace(expr).withText(qual.getText).highlightFrom(qual))
    case _ => None
  }
}

object RedundantLastOption extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("remove.redundant.lastOption")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.lastOption`(qual) if isOption(qual) =>
      Some(replace(expr).withText(qual.getText).highlightFrom(qual))
    case _ => None
  }
}