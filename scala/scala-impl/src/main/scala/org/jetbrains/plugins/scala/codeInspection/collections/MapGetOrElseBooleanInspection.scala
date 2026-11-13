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
class MapGetOrElseBooleanInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.map.and.getorelse.false.to.exists"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.options"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(MapGetOrElseFalse, MapGetOrElseTrue)
}

abstract class MapGetOrElseBoolean(defaultValue: String, newMethodName: String, override val hint: String) extends SimplificationType {
  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case qual `.map` (f@returnsBoolean()) `.getOrElse` (literal(`defaultValue`)) if isOption(qual) =>
        Some(replace(expr).withText(invocationText(qual, newMethodName, f)).highlightFrom(qual))
      case _ => None
    }
  }
}

object MapGetOrElseFalse extends MapGetOrElseBoolean("false", "exists", ScalaInspectionBundle.message("map.getOrElse.false.hint"))

object MapGetOrElseTrue extends MapGetOrElseBoolean("true", "forall", ScalaInspectionBundle.message("map.getOrElse.true.hint"))
