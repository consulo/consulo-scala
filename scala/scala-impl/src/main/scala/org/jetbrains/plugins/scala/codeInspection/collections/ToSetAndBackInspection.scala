package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScType, ScTypeExt}

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class ToSetAndBackInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.conversion.to.set.and.back.to.distinct"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(ToSetAndBackToDistinct)
}

object ToSetAndBackToDistinct extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.toSet.and.back.with.distinct")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    implicit val context: Context = Context(expr)

    def sameCollectionType(tp1: ScType, tp2: ScType): Boolean = {
      (tp1, tp2) match {
        case (ParameterizedType(des1, _), ParameterizedType(des2, _)) if des1.equiv(des2) => true
        case _ => false
      }
    }

    expr match {
      // TODO infix notation?
      case `.toCollection`(`.toSet` (qual@Typeable(qualType)), _*)
        if sameCollectionType(qualType, expr.`type`().getOrAny) && (isSeq(qual) || isArray(qual)) =>
        Some(replace(expr).withText(invocationText(qual, "distinct")).highlightFrom(qual))
      case _ => None
    }
  }

}
