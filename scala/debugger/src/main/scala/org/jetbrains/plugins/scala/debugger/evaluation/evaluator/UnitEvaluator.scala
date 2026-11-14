package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator
import consulo.internal.com.sun.jdi.VoidValue

object UnitEvaluator extends Evaluator {
  override def evaluate(context: EvaluationContextImpl): VoidValue =
    context.getDebugProcess.getVirtualMachineProxy.mirrorOfVoid()
}
