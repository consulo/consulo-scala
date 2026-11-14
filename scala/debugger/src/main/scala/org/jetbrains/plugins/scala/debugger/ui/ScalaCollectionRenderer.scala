package org.jetbrains.plugins.scala.debugger
package ui

import consulo.annotation.component.ExtensionImpl
import com.intellij.java.debugger.engine.evaluation.EvaluationContext
import com.intellij.java.debugger.impl.engine.evaluation.expression.{Evaluator, ExpressionEvaluatorImpl, IdentityEvaluator}
import com.intellij.java.debugger.engine.evaluation.expression.ExpressionEvaluator
import com.intellij.java.debugger.engine.{DebuggerUtils, SuspendContext}
import com.intellij.java.debugger.impl.engine.JVMNameUtil
import com.intellij.java.debugger.impl.ui.tree.render.{ChildrenBuilder, DescriptorLabelListener}
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import com.intellij.java.debugger.impl.ui.tree.ValueDescriptor
import consulo.util.dataholder.Key
import consulo.internal.com.sun.jdi._
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator._
import org.jetbrains.plugins.scala.debugger.filters.ScalaDebuggerSettings

import scala.jdk.CollectionConverters._

@ExtensionImpl(id = "scalaCollectionRenderer", order = "before scalaClassRenderer")
class ScalaCollectionRenderer extends ScalaClassRenderer {

  import ScalaCollectionRenderer._

  override def getName: String = DebuggerBundle.message("scala.collection.renderer")

  override def isApplicableFor(tpe: Type): Boolean =
    tpe match {
      case ct: ClassType if isCollection(ct) => !ScalaClassRenderer.isStringBuilder(ct)
      case _ => false
    }

  override def isEnabled: Boolean =
    ScalaDebuggerSettings.getInstance().FRIENDLY_COLLECTION_DISPLAY_ENABLED

  // upstream: the renderer API is asynchronous, consulo-java renderers are called on the debugger manager thread
  override def isExpandable(value: Value, context: EvaluationContext, parentDescriptor: NodeDescriptor): Boolean =
    evaluateNonEmpty(value.asInstanceOf[ObjectReference], context)

  override def calcLabel(descriptor: ValueDescriptor, context: EvaluationContext, listener: DescriptorLabelListener): String = {
    val ref = descriptor.getValue.asInstanceOf[ObjectReference]
    val hasDefiniteSize = evaluateHasDefiniteSize(ref, context)
    val size = if (hasDefiniteSize) evaluateSize(ref, context).toString else "?"
    s"size = $size"
  }

  override protected def doBuildChildren(value: Value, builder: ChildrenBuilder, context: EvaluationContext): Unit = {
    val ref = value.asInstanceOf[ObjectReference]
    val startIndex = nextIndex(builder, context)
    if (evaluateHasDefiniteSize(ref, context)) renderStrictCollection(ref, builder, context, startIndex)
    else renderNonStrictCollection(ref, builder, context, startIndex)
  }

  private def renderStrictCollection(ref: ObjectReference, builder: ChildrenBuilder, context: EvaluationContext, index: Int): Unit = {
    val vector = evaluateVectorSlice(ref, index, 100, context)
    val vectorSize = evaluateSize(vector, context)
    val descs = (0 until vectorSize).map { i =>
      val v = evaluateVectorApply(vector, i, context)
      val desc = new CollectionElementDescriptor(context.getProject, i + index, v)
      builder.getNodeManager.createNode(desc, context)
    }
    val size = evaluateSize(ref, context)
    val remaining = math.max(size - vectorSize - index, 0)
    if (remaining == 0)
      builder.addChildren(descs.asJava, true)
    else {
      builder.addChildren(descs.asJava, false)
      setNextIndex(builder, context, index + vectorSize)
      builder.tooManyChildren(remaining)
    }
  }

  private def renderNonStrictCollection(ref: ObjectReference, builder: ChildrenBuilder, context: EvaluationContext, index: Int): Unit = {
    val newIndex = index + 10
    val nodes = (index until newIndex).iterator.map { n =>
      val vector = evaluateVectorSlice(ref, n, 1, context)
      if (evaluateSize(vector, context) == 1) {
        val desc = new CollectionElementDescriptor(context.getProject, n, evaluateVectorApply(vector, 0, context))
        Some(builder.getNodeManager.createNode(desc, context))
      } else None
    }.takeWhile(_.isDefined).flatten.toList

    val hasDefiniteSize = evaluateHasDefiniteSize(ref, context)
    if (!hasDefiniteSize && nodes.size == newIndex - index) {
      builder.addChildren(nodes.asJava, false)
      setNextIndex(builder, context, newIndex)
      builder.tooManyChildren(-1)
    } else {
      builder.addChildren(nodes.asJava, true)
    }
  }
}

private[debugger] object ScalaCollectionRenderer {
  // upstream: `tooManyChildren` takes a callback rendering the next part; in Consulo the children are computed again,
  // so the index of the next part is stored in the parent descriptor for the current suspend context
  private val NextIndexKey: Key[(SuspendContext, Int)] = Key.create("scala.collection.renderer.next.index")

  private def nextIndex(builder: ChildrenBuilder, context: EvaluationContext): Int =
    builder.getParentDescriptor.getUserData(NextIndexKey) match {
      case (suspendContext, index) if suspendContext eq context.getSuspendContext => index
      case _ => 0
    }

  private def setNextIndex(builder: ChildrenBuilder, context: EvaluationContext, index: Int): Unit =
    builder.getParentDescriptor.putUserData(NextIndexKey, (context.getSuspendContext, index))

  private def isView(ct: ClassType): Boolean =
    DebuggerUtils.instanceOf(ct, "scala.collection.View") ||
      DebuggerUtils.instanceOf(ct, "scala.collection.IterableView")

  private def isCollection(ct: ClassType): Boolean =
    DebuggerUtils.instanceOf(ct, "scala.collection.Iterable") && !isView(ct)

  def isSeq(ct: ClassType): Boolean =
    DebuggerUtils.instanceOf(ct, "scala.collection.Seq") && !isView(ct)

  def evaluateHasDefiniteSize(ref: ObjectReference, context: EvaluationContext): Boolean =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      "hasDefiniteSize",
      JVMNameUtil.getJVMRawText("()Z"),
      Seq.empty
    ).asExpressionEvaluator.evaluate(context).asInstanceOf[BooleanValue].value()

  def evaluateSize(ref: ObjectReference, context: EvaluationContext): Int =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      "size",
      JVMNameUtil.getJVMRawText("()I"),
      Seq.empty
    ).asExpressionEvaluator.evaluate(context).asInstanceOf[IntegerValue].value()

  def evaluateNonEmpty(ref: ObjectReference, context: EvaluationContext): Boolean =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      "nonEmpty",
      JVMNameUtil.getJVMRawText("()Z"),
      Seq.empty
    ).asExpressionEvaluator.evaluate(context).asInstanceOf[BooleanValue].value()

  private def evaluateIterableOperation(name: String)(ref: ObjectReference, n: Int, context: EvaluationContext): ObjectReference =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      name,
      null, // Different collection types have different `take` method signatures, depending on specializations, search only by name
      Seq(new IntEvaluator(n))
    ).asExpressionEvaluator.evaluate(context).asInstanceOf[ObjectReference]

  private def evaluateDrop(ref: ObjectReference, n: Int, context: EvaluationContext): ObjectReference =
    evaluateIterableOperation("drop")(ref, n, context)

  private def evaluateTake(ref: ObjectReference, n: Int, context: EvaluationContext): ObjectReference =
    evaluateIterableOperation("take")(ref, n, context)

  private def evaluateToVector(ref: ObjectReference, context: EvaluationContext): ObjectReference =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      "toVector",
      JVMNameUtil.getJVMRawText("()Lscala/collection/immutable/Vector;"),
      Seq.empty
    ).asExpressionEvaluator.evaluate(context).asInstanceOf[ObjectReference]

  def evaluateVectorSlice(ref: ObjectReference, offset: Int, length: Int, context: EvaluationContext): ObjectReference = {
    val dropped = evaluateDrop(ref, offset, context)
    val taken = evaluateTake(dropped, length, context)
    evaluateToVector(taken, context)
  }

  def evaluateVectorApply(ref: ObjectReference, n: Int, context: EvaluationContext): Value =
    ScalaMethodEvaluator(
      new IdentityEvaluator(ref),
      "apply",
      JVMNameUtil.getJVMRawText("(I)Ljava/lang/Object;"),
      Seq(new IntEvaluator(n))
    ).asExpressionEvaluator.evaluate(context)

  private implicit class EvaluatorToExpressionEvaluatorOps(private val evaluator: Evaluator) extends AnyVal {
    def asExpressionEvaluator: ExpressionEvaluator = new ExpressionEvaluatorImpl(evaluator)
  }
}
