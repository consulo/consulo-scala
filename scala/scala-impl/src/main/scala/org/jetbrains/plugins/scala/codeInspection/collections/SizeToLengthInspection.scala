package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class SizeToLengthInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.size.to.length.on.arrays.and.strings"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.size"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(SizeToLength)
}

object SizeToLength extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("size.to.length")
  val `.size`: Qualified = invocation("size").from(likeCollectionClasses)

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      // TODO infix notation?
      case `.size`(qual@Typeable(tpe)) if isArray(qual) || isString(tpe) =>
        Some(replace(expr).withText(invocationText(qual, "length")).highlightFrom(qual))
      case _ => None
    }
  }
  
  def isString(tp: ScType): Boolean = {
    val canonicalText = tp.widen.canonicalText
    canonicalText == "_root_.java.lang.String" || canonicalText == "_root_.scala.Predef.String"
  }
}
