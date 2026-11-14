package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.engine.evaluation.expression.Modifier
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator

// upstream: platform ModifiableEvaluator/ModifiableValue, consulo-java Evaluator still exposes the modifier via getModifier
final class ModifiableValue(value: AnyRef, modifier: Modifier) {
  def getValue: AnyRef = value

  def getModifier: Modifier = modifier
}

trait ModifiableEvaluator extends Evaluator {
  @volatile private var myModifier: Modifier = _

  def evaluateModifiable(context: EvaluationContextImpl): ModifiableValue

  override final def evaluate(context: EvaluationContextImpl): AnyRef = {
    val result = evaluateModifiable(context)
    myModifier = result.getModifier
    result.getValue
  }

  override def getModifier: Modifier = myModifier
}

object ModifiableEvaluator {
  extension (evaluator: Evaluator) {
    def evaluateModifiable(context: EvaluationContextImpl): ModifiableValue = evaluator match {
      case modifiable: ModifiableEvaluator => modifiable.evaluateModifiable(context)
      case _ =>
        val value = evaluator.evaluate(context)
        new ModifiableValue(value, evaluator.getModifier)
    }
  }
}
