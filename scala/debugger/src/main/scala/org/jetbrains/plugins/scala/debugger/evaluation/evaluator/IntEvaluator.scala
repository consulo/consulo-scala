package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator
import consulo.internal.com.sun.jdi.IntegerValue

private[debugger] final class IntEvaluator(n: Int) extends Evaluator {
  override def evaluate(context: EvaluationContextImpl): IntegerValue =
    context.getDebugProcess.getVirtualMachineProxy.mirrorOf(n)
}
