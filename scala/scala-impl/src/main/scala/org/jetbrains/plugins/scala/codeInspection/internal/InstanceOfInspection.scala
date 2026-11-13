package org.jetbrains.plugins.scala.codeInspection.internal

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.collections.{OperationOnCollectionInspection, Qualified, Simplification, SimplificationType, invocation, invocationText}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.api.isUnitTestMode
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScTypeArgument, ScTypeElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScGenericCall}
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt}
import org.jetbrains.plugins.scala.project.ProjectExt

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class InstanceOfInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("internal.instanceOf.shouldBe.is"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.internal"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(InstanceOfShouldBeIsInspection)
}

object InstanceOfShouldBeIsInspection extends SimplificationType() {
  override val hint: String = ScalaInspectionBundle.message("replace.with.is")

  private val `.isInstanceOf`: Qualified = invocation("isInstanceOf")

  private def typeArgConformsToBaseExprType(base: ScExpression, targ: ScTypeArgument): Boolean = {
    val conforms =
      for {
        baseType <- base.`type`().toOption.map(_.widen)
        targType <- targ.typeElement.map(_.calcType)
      } yield targType.conforms(baseType)

    conforms.getOrElse(false)
  }

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    implicit val context: Context = Context(expr)

    expr match {
      case _ if !expr.getProject.isIntellijScalaPluginProject && !isUnitTestMode => None
      case `.isInstanceOf`(base) & ScGenericCall(_, Seq(targ)) if typeArgConformsToBaseExprType(base, targ) =>
        Some(replace(expr).withText(invocationText(base, "is") + s"[${targ.getText}]").highlightRef)
      case _ =>
        None
    }
  }
}
