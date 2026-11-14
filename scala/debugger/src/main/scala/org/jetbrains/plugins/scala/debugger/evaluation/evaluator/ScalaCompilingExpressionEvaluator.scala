package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.engine.evaluation.expression.{ExpressionEvaluator, Modifier}
import com.intellij.java.debugger.engine.evaluation.EvaluationContext
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import consulo.internal.com.sun.jdi.Value

class ScalaCompilingExpressionEvaluator(evaluator: ScalaCompilingEvaluator) extends ExpressionEvaluator {
  override def evaluate(context: EvaluationContext): Value = evaluator.evaluate(context.asInstanceOf[EvaluationContextImpl])

  override def getModifier: Modifier = null
}
