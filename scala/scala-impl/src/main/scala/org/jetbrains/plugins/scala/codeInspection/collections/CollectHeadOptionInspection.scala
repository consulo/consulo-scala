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
class CollectHeadOptionInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.collect.and.headoption.to.collectfirst"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(CollectHeadOption)
}

object CollectHeadOption extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.collect.headOption.with.collectFirst")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      // TODO infix notation?
      case `.headOption`(qual `.collect` (f)) =>
        Some(replace(expr).withText(invocationText(qual, "collectFirst", f)).highlightFrom(qual))
      case _ => None
    }
  }

}
