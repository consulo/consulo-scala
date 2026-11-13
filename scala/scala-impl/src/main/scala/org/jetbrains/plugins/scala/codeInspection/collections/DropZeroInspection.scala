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
class DropZeroInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.drop.with.zero.has.no.effect"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.filter.and.exists"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: Seq[SimplificationType] = {
    ArraySeq(DropZeroOption)
  }
}

object DropZeroOption extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("drop.with.0.is.redundant")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual`.drop` arg if arg.textMatches("0") =>
        Some(replace(expr).withText(qual.getText).highlightFrom(qual))
      case _ => None
    }
  }
}
