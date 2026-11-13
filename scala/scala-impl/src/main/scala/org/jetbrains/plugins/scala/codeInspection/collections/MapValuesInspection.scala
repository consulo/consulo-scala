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
class MapValuesInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.extracting.values.manually"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.maps"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(MapValues)
}

object MapValues extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.with.values")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.toIterator`(qual`.map`(`_._2`())) if isMap(qual) =>
      val iteratorHint = ScalaInspectionBundle.message("replace.with.valuesIterator")
      Some(replace(expr).withText(invocationText(qual, "valuesIterator")).highlightFrom(qual).withHint(iteratorHint))
    case qual`.map`(`_._2`()) if isMap(qual) =>
      Some(replace(expr).withText(invocationText(qual, "values")).highlightFrom(qual))
    case _ => None
  }
}
