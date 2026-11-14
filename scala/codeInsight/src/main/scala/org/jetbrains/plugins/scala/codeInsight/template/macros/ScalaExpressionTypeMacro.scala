package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.{Expression, ExpressionContext, Result}

@ExtensionImpl
final class ScalaExpressionTypeMacro extends ScalaMacro {

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = params match {
    case Array(param) =>
      val maybeType = resultToScExpr(param.calculateResult(context))(using context)
      maybeType.map(ScalaTypeResult.apply).orNull
    case _            => null
  }

  override def getNameShort: String = "expressionType"

  override def getPresentableName: LocalizeValue = LocalizeValue.localizeTODO("expressionType(Expression)")
}
