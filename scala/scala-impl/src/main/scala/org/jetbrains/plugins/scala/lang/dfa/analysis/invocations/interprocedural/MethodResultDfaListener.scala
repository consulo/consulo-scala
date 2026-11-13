package org.jetbrains.plugins.scala.lang.dfa.analysis.invocations.interprocedural

import consulo.language.dataFlow.ir.DfaAnchor
import consulo.language.dataFlow.interpreter.{DfaListener, UnsatisfiedConditionProblem}
import consulo.language.dataFlow.memory.DfaMemoryState
import consulo.language.dataFlow.types.DfType
import consulo.language.dataFlow.value.DfaValue
import consulo.util.lang.ThreeState

class MethodResultDfaListener(resultDestination: DfaValue) extends DfaListener {

  private var resultValue: Option[DfType] = None

  def collectResultValue: DfType = resultValue match {
    case Some(DfType.TOP | DfType.BOTTOM) => DfType.TOP
    case Some(other) => other
    case _ => DfType.TOP
  }

  override def beforeAssignment(source: DfaValue, destination: DfaValue, state: DfaMemoryState, anchor: DfaAnchor): Unit = {
    if (destination == resultDestination) {
      val newValue = state.getDfType(source)
      resultValue = if (resultValue.contains(DfType.BOTTOM) || newValue == DfType.BOTTOM) Some(DfType.BOTTOM)
      else resultValue.map(_.join(newValue)).orElse(Some(newValue))
    }
  }

  override def beforePush(args: Array[DfaValue], value: DfaValue, anchor: DfaAnchor, state: DfaMemoryState): Unit = ()

  override def onCondition(unsatisfiedCondition: UnsatisfiedConditionProblem, value: DfaValue,
                           failed: ThreeState, state: DfaMemoryState): Unit = ()
}
