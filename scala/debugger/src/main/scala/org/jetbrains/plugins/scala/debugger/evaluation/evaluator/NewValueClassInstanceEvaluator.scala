package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.localize.JavaDebuggerLocalize
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.evaluation.expression.Evaluator
import consulo.internal.com.sun.jdi.{ClassType, ObjectReference, Value}
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.evaluation.EvaluationException

import scala.jdk.CollectionConverters._
import scala.util.Try

private[debugger] final class NewValueClassInstanceEvaluator(typeEvaluator: ScalaTypeEvaluator, param: Evaluator) extends Evaluator {

  override def evaluate(context: EvaluationContextImpl): ObjectReference = {
    val debugProcess = context.getDebugProcess
    val ct = typeEvaluator.evaluate(context) match {
      case c: ClassType => c
      case _ =>
        throw EvaluationException(JavaDebuggerLocalize.evaluationErrorCannotEvaluateClassType().get())
    }

    val ctor = Try(ct.methodsByName("<init>").get(0))
      .getOrElse(throw EvaluationException(DebuggerBundle.message("error.cannot.resolve.value.class.primary.constructor", ct)))

    val arg = Option(param.evaluate(context)).collect { case v: Value => v }.orNull

    debugProcess.newInstance(context, ct, ctor, List(arg).asJava)
  }
}
