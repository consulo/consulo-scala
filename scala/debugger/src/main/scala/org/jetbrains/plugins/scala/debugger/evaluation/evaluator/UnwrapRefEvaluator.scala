package org.jetbrains.plugins.scala
package debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator
import org.jetbrains.plugins.scala.debugger.evaluation.util.DebuggerUtil

class UnwrapRefEvaluator(parent: Evaluator) extends Evaluator {

  override def evaluate(context: EvaluationContextImpl): AnyRef = {
    val value = parent.evaluate(context)
    DebuggerUtil.unwrapScalaRuntimeRef(value)
  }
}


