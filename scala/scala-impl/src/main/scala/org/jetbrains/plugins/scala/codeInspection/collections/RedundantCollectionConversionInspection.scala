package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.inspection.ProblemHighlightType
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt}
import org.jetbrains.plugins.scala.lang.psi.types.result._

import scala.collection.immutable.ArraySeq

object RedundantCollectionConversion extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("redundant.collection.conversion")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    implicit val context: Context = Context(expr)

    val typeAfterConversion = expr.`type`().getOrAny

    // note:
    // will match <Seq(1, 2).to> and <Seq(1, 2).to[List]> but not <Seq(1, 2).to>[List]
    // because of a check in MethodRepr in `.toCollection`
    expr match {
      // TODO infix notation?
      case `.toCollection`(base@Typeable(baseType)) if baseType.conforms(typeAfterConversion) =>
        val simplification = replace(expr).withText(base.getText).highlightFrom(base)
        Some(simplification)
      case _ => None
    }
  }
}

@ExtensionImpl
final class RedundantCollectionConversionInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.collection.conversion"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.collections"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override protected def problemHighlightType: ProblemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(RedundantCollectionConversion)
}
