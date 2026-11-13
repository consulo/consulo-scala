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
class ReplaceToWithUntilInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.replace.to.with.until"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(ReplaceToWithUntil)
}

object ReplaceToWithUntil extends SimplificationType {

  override def hint: String = ScalaInspectionBundle.message("replace.to.with.until")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.to`(x `-` literal("1")) =>
        Some(replace(expr).withText(invocationText(qual, "until", x)).highlightFrom(qual))
      case _ => None
    }
  }
}