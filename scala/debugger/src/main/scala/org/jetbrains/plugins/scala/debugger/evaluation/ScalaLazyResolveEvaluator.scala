package org.jetbrains.plugins.scala.debugger.evaluation

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.engine.evaluation.expression.ExpressionEvaluator
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator.{ModifiableEvaluator, ModifiableValue}
import consulo.language.psi.PsiModificationTracker
import org.jetbrains.plugins.scala.debugger.evaluation.ReadActions.inNonBlockingReadAction
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment

private final class ScalaLazyResolveEvaluator(
  codeFragment: ScalaCodeFragment,
  position: SourcePosition,
  compileIfNeeded: Boolean = true
) extends ModifiableEvaluator {
  // The evaluator built from the code fragment together with the PSI modification count it was built at. The platform
  // evaluates the same instance repeatedly (every hit of a conditional breakpoint, retries), and resolving the fragment
  // again each time is the expensive part. Only used on the debugger manager thread, volatile so that a stray race
  // costs no more than an extra build.
  @volatile private var cached: Option[(modCount: Long, evaluator: ExpressionEvaluator)] = None

  override def evaluateModifiable(context: EvaluationContextImpl): ModifiableValue = {
    val evaluator = evaluatorFor(context)
    val value = evaluator.evaluate(context)
    val modifier = evaluator.getModifier
    new ModifiableValue(value, modifier)
  }

  private def evaluatorFor(context: EvaluationContextImpl): ExpressionEvaluator = {
    val modificationCount = PsiModificationTracker.getInstance(context.getProject).getModificationCount
    cached match {
      case Some((`modificationCount`, evaluator)) => evaluator
      case _ =>
        val evaluator = createEvaluator(context)
        cached = Some((modificationCount, evaluator))
        evaluator
    }
  }

  private def createEvaluator(context: EvaluationContextImpl): ExpressionEvaluator = {
    val process = context.getDebugProcess
    inNonBlockingReadAction(expireWhen = () => !process.isAttached) {
      if (compileIfNeeded) ScalaEvaluatorBuilder.build(codeFragment, position)
      else ScalaEvaluatorBuilder.buildWithoutCompilation(codeFragment, position)
    }
  }
}
