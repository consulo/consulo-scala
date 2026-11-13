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
class MapToBooleanContainsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.map.and.contains.true.false.to.exists.or.forall"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(MapContainsFalse, MapContainsTrue)
}

object MapContainsFalse extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.map.contains.false.with.not.forall")
  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.map`(pred @ returnsBoolean())`.contains`(literal("false")) =>
        val notForallText = invocationText(negation = true, qual, "forall", pred)
        Some(replace(expr).withText(notForallText).highlightFrom(qual))
      case _ => None
    }
  }
}

object MapContainsTrue extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.map.contains.true.with.exists")
  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case qual`.map`(pred @ returnsBoolean())`.contains`(literal("true")) =>
      val existsText = invocationText(qual, "exists", pred)
      Some(replace(expr).withText(existsText).highlightFrom(qual))
    case _ => None
  }
}