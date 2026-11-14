package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.{Expression, ExpressionContext, Result}
import consulo.language.editor.localize.CodeInsightLocalize

@ExtensionImpl
final class ScalaIterableComponentTypeMacro extends ScalaMacro {

  override def getNameShort: String = "iterableComponentType"
  
  override def getPresentableName: LocalizeValue = CodeInsightLocalize.macroIterableComponentType()

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = params match {
    case Array(head) =>
      Option(head.calculateResult(context))
        .flatMap(resultToScExpr(_)(using context))
        .flatMap { exprType =>
          arrayComponent(exprType).orElse {
            Some(exprType).filter(ScalaVariableOfTypeMacro.isIterable)
          }
        }.map(ScalaTypeResult.apply).orNull
    case _ => null
  }

  override def calculateQuickResult(params: Array[Expression], context: ExpressionContext): Result = calculateResult(params, context)

  override def getDefaultValue: String = ScalaMacro.DefaultValue
}
