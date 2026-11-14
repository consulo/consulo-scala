package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.impl.engine.evaluation.expression.TypeEvaluator
import com.intellij.java.debugger.engine.evaluation.EvaluateException
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.engine.{JVMName, JVMNameUtil}
import consulo.internal.com.sun.jdi.ReferenceType
import org.jetbrains.plugins.scala.rt.util.ScalaBytecodeConstants.TraitImplementationClassSuffix_211

/**
 * This is a workaround for scala versions > 2.11 (see SCL-10132).
 * It's required because in [[org.jetbrains.plugins.scala.debugger.evaluation.util.DebuggerUtil.classnamePostfix]]
 * we build append "$class" for all scala versions, even though it's actual only for 2.11 and earlier.
 */
class ScalaTypeEvaluator(jvmName: JVMName) extends TypeEvaluator(jvmName) {
  override def evaluate(context: EvaluationContextImpl): ReferenceType = {
    try super.evaluate(context).asInstanceOf[ReferenceType]
    catch {
      case e: EvaluateException =>
        val targetExc = e.getExceptionFromTargetVM
        if (targetExc != null && targetExc.referenceType().name == "java.lang.ClassNotFoundException") {
          val debugProcess = context.getDebugProcess
          val name = jvmName.getName(debugProcess)
          if (name.endsWith(TraitImplementationClassSuffix_211)) {
            val nameSince212 = JVMNameUtil.getJVMRawText(name.stripSuffix(TraitImplementationClassSuffix_211))
            new TypeEvaluator(nameSince212).evaluate(context).asInstanceOf[ReferenceType]
          }
          else throw e
        }
        else throw e
    }
  }
}
