package org.jetbrains.plugins.scala
package debugger.ui

import consulo.annotation.component.ExtensionImpl
import com.intellij.java.debugger.engine.evaluation.{EvaluateException, EvaluationContext}
import com.intellij.java.debugger.engine.DebugProcess
import com.intellij.java.debugger.impl.engine.DebugProcessImpl
import com.intellij.java.debugger.impl.ui.tree.render.{ChildrenBuilder, DescriptorLabelListener}
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import com.intellij.java.debugger.impl.ui.tree.ValueDescriptor
import consulo.execution.debug.ui.XDebuggerUIConstants
import consulo.internal.com.sun.jdi._
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.evaluation.util.DebuggerUtil
import org.jetbrains.plugins.scala.debugger.filters.ScalaDebuggerSettings
import org.jetbrains.plugins.scala.debugger.ui.util._

import java.util.concurrent.CompletableFuture

@ExtensionImpl(id = "scalaRuntimeRefRenderer")
class ScalaRuntimeRefRenderer extends ScalaClassRenderer {
  import ScalaRuntimeRefRenderer._

  override def getName: String = DebuggerBundle.message("scala.runtime.ref.renderer")

  setIsApplicableChecker(tpe => CompletableFuture.completedFuture(isApplicableFor(tpe)))

  // most Scala runtime refs are defined as Java classes
  override def isApplicableFor(tpe: Type): Boolean = tpe match {
    case ct: ClassType if ScalaDebuggerSettings.getInstance().DONT_SHOW_RUNTIME_REFS && isRuntimeRef(ct) => true
    case _ => false
  }

  override def shouldDisplay(context: EvaluationContext, objInstance: ObjectReference, field: Field): Boolean = false

  override def calcLabel(descriptor: ValueDescriptor, context: EvaluationContext, listener: DescriptorLabelListener): String = {
    onDebuggerManagerThread(context)(unwrapValue(descriptor.getValue).toString).attempt.flatTap {
      case Left(t) =>
        onDebuggerManagerThread(context) {
          descriptor.setValueLabelFailed(evaluationExceptionFromThrowable(t))
          listener.labelChanged()
        }
      case Right(label) =>
        onDebuggerManagerThread(context) {
          descriptor.setValueLabel(label)
          listener.labelChanged()
        }
    }.rethrow.getNow(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE.get())
  }

  override protected def buildChildrenAsync(value: Value, builder: ChildrenBuilder, context: EvaluationContext): CompletableFuture[Unit] =
    for {
      unwrapped <- onDebuggerManagerThread(context)(unwrapValue(value))
      renderer <- onDebuggerManagerThread(context)(context.getDebugProcess.asInstanceOf[DebugProcessImpl].getAutoRendererAsync(unwrapped.`type`())).flatten
      _ <- onDebuggerManagerThread(context)(renderer.buildChildren(unwrapped, builder, context))
    } yield ()

  override def isExpandableAsync(value: Value, context: EvaluationContext, descriptor: NodeDescriptor): CompletableFuture[java.lang.Boolean] =
    for {
      unwrapped <- onDebuggerManagerThread(context)(unwrapValue(value))
      renderer <- onDebuggerManagerThread(context)(context.getDebugProcess.asInstanceOf[DebugProcessImpl].getAutoRendererAsync(unwrapped.`type`())).flatten
      result <- onDebuggerManagerThread(context)(renderer.isExpandableAsync(unwrapped, context, descriptor)).flatten
    } yield result

  // upstream: calcIdLabel(descriptor, process, labelListener)
  override def getIdLabel(value: Value, process: DebugProcess): String =
    value match {
      case null => super.getIdLabel(value, process)
      case _ =>
        val RefType(refType) = value.`type`(): @unchecked
        s"unwrapped Scala runtime $refType reference"
    }

  private def unwrapValue(ref: Value): Value = {
    ref match {
      case _: ObjectReference =>
        DebuggerUtil.unwrapScalaRuntimeRef(ref) match {
          case v: Value => v
          case _ => ref
        }
      case _ => throw new EvaluateException("Should not unwrap non reference value")
    }
  }
}

private object ScalaRuntimeRefRenderer {
  def isRuntimeRef(ct: ClassType): Boolean = {
    val name = ct.name()
    name.startsWith("scala.runtime") && name.endsWith("Ref") && name != "scala.runtime.LazyRef"
  }

  object RefType {
    def unapply(tpe: Type): Some[String] = {
      val name = tpe.name().replace("scala.runtime.", "").replace("Ref", "")
      if (name.startsWith("Volatile"))
        Some(s"volatile ${name.replace("Volatile", "")}")
      else
        Some(name)
    }
  }
}
