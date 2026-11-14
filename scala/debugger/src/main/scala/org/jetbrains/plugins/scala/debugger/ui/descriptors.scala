package org.jetbrains.plugins.scala.debugger.ui

import com.intellij.java.debugger.DebuggerContext
import com.intellij.java.debugger.engine.evaluation.EvaluateException
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.jdi.LocalVariableProxyImpl
import com.intellij.java.debugger.impl.ui.impl.watch.{ArrayElementDescriptorImpl, LocalVariableDescriptorImpl, ValueDescriptorImpl}
import com.intellij.java.debugger.impl.ui.tree.render.OnDemandRenderer
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import consulo.project.Project
import com.intellij.java.language.psi.PsiExpression
import consulo.internal.com.sun.jdi._
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.evaluation.EvaluationException

import java.util.Collections
import scala.collection.mutable
import scala.jdk.CollectionConverters._

private object LazyValDescriptor {
  def create(project: Project, ref: ObjectReference, field: Field, evaluationContext: EvaluationContextImpl): NodeDescriptor = {
    val allFields = ref.referenceType().allFields().asScala
    val allMethods = ref.referenceType().allMethods().asScala
    val isScala3 = allFields.exists(f => ScalaClassRenderer.isVarHandleOrOffsetField(f.name()))
    if (isScala3) createLazyValDescriptorScala3(project, ref, field, allFields, allMethods, evaluationContext)
    else createLazyValDescriptorScala2(project, ref, field, allFields, allMethods)
  }

  private def createLazyValDescriptorScala2(
    project: Project,
    ref: ObjectReference,
    field: Field,
    allFields: mutable.Buffer[Field],
    allMethods: mutable.Buffer[Method]
  ): NodeDescriptor = {
    val lazyValFields = allFields.filter(ScalaClassRenderer.isLazyVal(ref, _))
    val index = lazyValFields.indexOf(field)
    val bitmapNumber = index / 64
    val bitmapExponent = index % 64
    val transient = if (field.isTransient) "trans$" else ""
    val bitmapName = s"bitmap$$$transient$bitmapNumber"
    val bitmapField = allFields.find(_.name() == bitmapName).getOrElse(throw EvaluationException(DebuggerBundle.message("could.not.find.bitmap.field", bitmapName)))
    val bitmapValue = ref.getValue(bitmapField).asInstanceOf[PrimitiveValue].longValue()
    val bitmapMask = 1L << bitmapExponent
    val isInitialized = (bitmapValue & bitmapMask) != 0
    val init = findInitializerScala2(field, allMethods)
    if (isInitialized) new FieldLazyValDescriptor(project, ref, init)
    else new NotInitializedFieldLazyValDescriptor(project, ref, init)
  }

  private def createLazyValDescriptorScala3(
    project: Project,
    ref: ObjectReference,
    field: Field,
    allFields: mutable.Buffer[Field],
    allMethods: mutable.Buffer[Method],
    evaluationContext: EvaluationContextImpl
  ): NodeDescriptor = {
    val lazyValFields = allFields.filter(ScalaClassRenderer.isLazyVal(ref, _))
    val index = lazyValFields.indexOf(field)
    val bitmapIndex = index / 32
    val bitmapExponent = index % 32
    val bitmapName = s"${bitmapIndex}bitmap$$"
    val bitmapField = allFields.find(_.name().contains(bitmapName))
    val init = findInitializerScala3(field, allMethods)

    bitmapField match {
      case Some(bitmap) =>
        // Old lazy val encoding, Scala 3.0, 3.1 and 3.2
        val bitmapValue = ref.getValue(bitmap).asInstanceOf[PrimitiveValue].longValue()
        val bitmapMask = (1L << bitmapExponent) + (1L << (bitmapExponent + 1))
        val isInitialized = (bitmapValue & bitmapMask) == bitmapMask
        if (isInitialized) new FieldLazyValDescriptor(project, ref, init)
        else new NotInitializedFieldLazyValDescriptor(project, ref, init)
      case None =>
        // New lazy val encoding, Scala 3.3+
        ref.getValue(field) match {
          case null =>
            val frameProxy = evaluationContext.getFrameProxy
            val isInitializable =
              if (frameProxy eq null)
                false
              else
                frameProxy.threadProxy().frames().asScala.forall { frame =>
                  val methodName = frame.location().method().name()
                  !methodName.contains("$lzyINIT")
                }

            if (isInitializable) new NotInitializedFieldLazyValDescriptor(project, ref, init)
            else new IntermediateStateFieldLazyValDescriptor(project, ref, init, DebuggerBundle.message("lazy.val.not.initialized"))
          case obj: ObjectReference =>
            val name = obj.referenceType().name()
            name match {
              case "scala.runtime.LazyVals$Evaluating$" | "scala.runtime.LazyVals$Waiting" =>
                new IntermediateStateFieldLazyValDescriptor(project, ref, init, DebuggerBundle.message("lazy.val.being.initialized"))
              case _ =>
                new FieldLazyValDescriptor(project, ref, init)
            }
        }
    }
  }

  private def findInitializerScala2(field: Field, allMethods: mutable.Buffer[Method]): Method = {
    val name = field.name()
    allMethods.find(_.name() == field.name()).getOrElse(throw EvaluationException(DebuggerBundle.message("could.not.find.accessor.method", name)))
  }

  private def findInitializerScala3(field: Field, allMethods: mutable.Buffer[Method]): Method = {
    val name = field.name().split("\\$lzy")(0)
    allMethods.find(_.name() == name).getOrElse(throw EvaluationException(DebuggerBundle.message("could.not.find.accessor.method", name)))
  }
}

private class FieldLazyValDescriptor(project: Project, ref: ObjectReference, init: Method)
  extends ValueDescriptorImpl(project) {

  override def getName: String = init.name()

  override def getType: Type = init.returnType()

  override def calcValue(evaluationContext: EvaluationContextImpl): Value =
    evaluationContext.getDebugProcess.invokeMethod(evaluationContext, ref, init, Collections.emptyList())

  override def getDescriptorEvaluation(context: DebuggerContext): PsiExpression =
    throw new EvaluateException("Lazy val evaluation is not supported")

  def getObject: ObjectReference = ref
}

// upstream: a custom on-demand presentation ("lazy val not initialized" with an "initialize" link); Consulo shows
// the generic on-demand "evaluate" link
private final class NotInitializedFieldLazyValDescriptor(project: Project, ref: ObjectReference, init: Method)
  extends FieldLazyValDescriptor(project, ref, init) {

  OnDemandRenderer.ON_DEMAND_CALCULATED.set(this, false)
}

private final class IntermediateStateFieldLazyValDescriptor(project: Project, ref: ObjectReference, init: Method, @Nls presentation: String)
  extends FieldLazyValDescriptor(project, ref, init) {

  override def calcValue(evaluationContext: EvaluationContextImpl): Value =
    throw new EvaluateException(presentation)
}

private[debugger] class LocalLazyValDescriptor(project: Project, proxy: LocalVariableProxyImpl, lazyRefType: Type, init: Method)
  extends LocalVariableDescriptorImpl(project, proxy) {

  override def getName: String = {
    val name = super.getName
    if (name.endsWith("$lzy")) name.dropRight(4) else name
  }

  override def getType: Type = lazyRefType

  override def isPrimitive: Boolean = getType.name() != "scala.runtime.LazyRef"

  override def calcValue(context: EvaluationContextImpl): Value = {
    val lazyRefValue = proxy.getFrame.getValue(proxy)
    val thisObject = context.getThisObject.asInstanceOf[ObjectReference]
    val refType = init.declaringType()
    val args = Collections.singletonList(lazyRefValue)
    refType match {
      case ct: ClassType if init.isStatic => context.getDebugProcess.invokeMethod(context, ct, init, args)
      case it: InterfaceType if init.isStatic => context.getDebugProcess.invokeMethod(context, it, init, args)
      case _ => context.getDebugProcess.invokeMethod(context, thisObject, init, args)
    }
  }
}

private[debugger] final class NotInitializedLocalLazyValDescriptor(project: Project, proxy: LocalVariableProxyImpl, lazyRefType: Type, init: Method)
  extends LocalLazyValDescriptor(project, proxy, lazyRefType, init) {

  // upstream: a custom on-demand presentation with the lazy type; Consulo shows the generic on-demand "evaluate" link
  OnDemandRenderer.ON_DEMAND_CALCULATED.set(this, false)
}

private final class CollectionElementDescriptor(project: Project, index: Int, value: Value)
  extends ArrayElementDescriptorImpl(project, null, index) {

  override def calcValue(evaluationContext: EvaluationContextImpl): Value = value

  override def canSetValue: Boolean = false

  override def getDescriptorEvaluation(context: DebuggerContext): PsiExpression =
    throw EvaluationException(DebuggerBundle.message("collection.element.descriptors.evaluation.not.supported"))
}
