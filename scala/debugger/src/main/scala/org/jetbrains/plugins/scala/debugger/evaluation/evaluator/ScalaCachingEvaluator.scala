package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator

class ScalaCachingEvaluator(evaluator: Evaluator) extends Evaluator {
  private var result: Option[AnyRef] = None

  override def evaluate(context: EvaluationContextImpl): AnyRef = {
    result.getOrElse {
      val res = evaluator.evaluate(context)
      result = Some(res)
      res
    }
  }
}
