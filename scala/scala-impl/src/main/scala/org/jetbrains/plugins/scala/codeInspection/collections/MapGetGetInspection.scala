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
class MapGetGetInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.get.when.getting.a.value.from.map"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.maps"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(MapGetGet)
}

object MapGetGet extends SimplificationType() {

  override def hint: String = ScalaInspectionBundle.message("get.get.hint")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      // TODO infix notation?
      case `.get`(map`.getOnMap`(key)) =>
        Some(replace(expr)
          .withText(replacementText(map, key))
          .highlightFrom(map))
      case _ => None
    }
  }

  def replacementText(qual: ScExpression, keyArg: ScExpression): String = {
    val firstArgText = argListText(Seq(keyArg))
    s"${qual.getText}$firstArgText"
  }
}
