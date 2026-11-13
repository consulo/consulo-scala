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
class MapKeysInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.extracting.keys.manually"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.maps"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(MapKeys)
}

object MapKeys extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.with.keys")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    // TODO infix notation?
    case `.toIterator`(qual`.map` `_._1`()) if isMap(qual) =>
      val iteratorHint = ScalaInspectionBundle.message("replace.with.keysIterator")
      Some(replace(expr).withText(invocationText(qual, "keysIterator")).highlightFrom(qual).withHint(iteratorHint))
    case `.toSet`(qual`.map` `_._1`()) if isMap(qual) =>
      val setHint = ScalaInspectionBundle.message("replace.with.keySet")
      Some(replace(expr).withText(invocationText(qual, "keySet")).highlightFrom(qual).withHint(setHint))
    case qual`.map` `_._1`() if isMap(qual) =>
      Some(replace(expr).withText(invocationText(qual, "keys")).highlightFrom(qual))
    case _ => None
  }
}
