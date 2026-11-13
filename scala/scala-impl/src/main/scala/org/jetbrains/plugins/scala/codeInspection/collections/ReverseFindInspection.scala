package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class ReverseFindInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.reverse.find.to.findLast"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(ReverseFindInspection)
}

object ReverseFindInspection extends SimplificationType {

  override def hint: String = ScalaInspectionBundle.message("replace.with.findlast")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case `.reverse`(qual)`.find`(f) =>
        Some(replace(expr).withText(invocationText(qual, "findLast", f)).highlightRange(qual.endOffset, expr.endOffset))
      case _ => None
    }
  }
}