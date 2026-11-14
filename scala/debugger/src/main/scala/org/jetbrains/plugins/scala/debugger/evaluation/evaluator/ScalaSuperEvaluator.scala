package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator.ModifiableEvaluator.evaluateModifiable
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator.{ModifiableEvaluator, ModifiableValue}

trait ScSuperEvaluator

class ScalaSuperEvaluator(iterations: Int = 0) extends ScalaThisEvaluator(iterations) with ScSuperEvaluator

class ScalaSuperDelegate(delegate: Evaluator) extends ModifiableEvaluator with ScSuperEvaluator {
  override def evaluateModifiable(context: EvaluationContextImpl): ModifiableValue = delegate.evaluateModifiable(context)
}
