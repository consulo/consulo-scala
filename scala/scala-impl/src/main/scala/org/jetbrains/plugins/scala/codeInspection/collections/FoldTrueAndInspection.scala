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
class FoldTrueAndInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.fold.simplifiable.to.forall"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(FoldTrueAnd)
}

object FoldTrueAnd extends SimplificationType(){

  override def hint: String = ScalaInspectionBundle.message("fold.true.and.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case `.foldLeft`(_, literal("true"), andCondition(cond)) if hasSideEffects(cond) =>
        None
      case `.fold`(qual, literal("true"), andCondition(cond)) =>
        Some(replace(expr).withText(invocationText(qual, "forall", cond)).highlightFrom(qual))
      case _ => None
    }
  }
}
