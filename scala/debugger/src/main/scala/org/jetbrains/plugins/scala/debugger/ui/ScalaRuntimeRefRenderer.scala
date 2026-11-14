package org.jetbrains.plugins.scala
package debugger.ui

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.engine.evaluation.{EvaluateException, EvaluationContext}
import com.intellij.java.debugger.engine.DebugProcess
import com.intellij.java.debugger.impl.engine.DebugProcessImpl
import com.intellij.java.debugger.impl.ui.tree.render.{ChildrenBuilder, DescriptorLabelListener, NodeRenderer}
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import com.intellij.java.debugger.impl.ui.tree.ValueDescriptor
import com.intellij.java.debugger.impl.settings.NodeRendererSettings
import consulo.internal.com.sun.jdi._
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.evaluation.util.DebuggerUtil
import org.jetbrains.plugins.scala.debugger.filters.ScalaDebuggerSettings

import scala.jdk.CollectionConverters._

@ExtensionImpl(id = "scalaRuntimeRefRenderer", order = "before scalaCollectionRenderer")
class ScalaRuntimeRefRenderer extends ScalaClassRenderer {
  import ScalaRuntimeRefRenderer._

  override def getName: String = DebuggerBundle.message("scala.runtime.ref.renderer")

  // most Scala runtime refs are defined as Java classes
  override def isApplicableFor(tpe: Type): Boolean = tpe match {
    case ct: ClassType if ScalaDebuggerSettings.getInstance().DONT_SHOW_RUNTIME_REFS && isRuntimeRef(ct) => true
    case _ => false
  }

  override def shouldDisplay(context: EvaluationContext, objInstance: ObjectReference, field: Field): Boolean = false

  // upstream: the renderer API is asynchronous, consulo-java renderers are called on the debugger manager thread
  override def calcLabel(descriptor: ValueDescriptor, context: EvaluationContext, listener: DescriptorLabelListener): String =
    unwrapValue(descriptor.getValue).toString

  override protected def doBuildChildren(value: Value, builder: ChildrenBuilder, context: EvaluationContext): Unit = {
    val unwrapped = unwrapValue(value)
    rendererFor(unwrapped).buildChildren(unwrapped, builder, context)
  }

  override def isExpandable(value: Value, context: EvaluationContext, descriptor: NodeDescriptor): Boolean = {
    val unwrapped = unwrapValue(value)
    rendererFor(unwrapped).isExpandable(unwrapped, context, descriptor)
  }

  override def getIdLabel(value: Value, process: DebugProcess): String =
    value match {
      case null => super.getIdLabel(value, process)
      case _ =>
        val RefType(refType) = value.`type`(): @unchecked
        s"unwrapped Scala runtime $refType reference"
    }

  private def rendererFor(value: Value): NodeRenderer =
    NodeRendererSettings.getInstance().getAllRenderers.asScala
      .find(r => r.isEnabled && r.isApplicable(value.`type`()))
      .getOrElse(DebugProcessImpl.getDefaultRenderer(value))

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
