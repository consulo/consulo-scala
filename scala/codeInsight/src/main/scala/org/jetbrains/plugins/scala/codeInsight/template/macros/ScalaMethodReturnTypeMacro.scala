package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.{Expression, ExpressionContext, Result}
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.types.api.FunctionType
import org.jetbrains.plugins.scala.lang.psi.types.result._

@ExtensionImpl
final class ScalaMethodReturnTypeMacro extends ScalaMacro {

  override def getNameShort: String = "methodReturnType"

  override def getDefaultValue: String = ScalaMacro.DefaultValue

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = {
    val parentFunction = Option(PsiTreeUtil.getParentOfType(context.getPsiElementAtStartOffset, classOf[ScFunction]))
    parentFunction
      .map(_.`type`().getOrAny match {
        case FunctionType(rt, _) => rt
        case t => t
      })
      .map(ScalaTypeResult.apply)
      .orNull
  }
}
