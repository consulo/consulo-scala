package org.jetbrains.plugins.scala.debugger
package ui

import consulo.annotation.component.ExtensionImpl
import com.intellij.java.debugger.DebuggerContext
import com.intellij.java.debugger.engine.DebuggerUtils
import com.intellij.java.debugger.engine.evaluation.{EvaluateException, EvaluationContext}
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.settings.NodeRendererSettings
import com.intellij.java.debugger.impl.ui.impl.watch.FieldDescriptorImpl
import com.intellij.java.debugger.impl.ui.tree.render.{ChildrenBuilder, ClassRenderer, DescriptorLabelListener}
import com.intellij.java.debugger.impl.ui.tree.{DebuggerTreeNode, FieldDescriptor, ValueDescriptor}
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import com.intellij.java.debugger.localize.JavaDebuggerLocalize
import consulo.language.psi.PsiElement
import consulo.logging.Logger
import consulo.project.Project
import consulo.internal.com.sun.jdi._
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.debugger.ui.util.buildChildrenOrShowError
import org.jetbrains.plugins.scala.rt.util.ScalaBytecodeConstants

import java.util.Collections
import scala.jdk.CollectionConverters._
import scala.util.Try

@ExtensionImpl(id = "scalaClassRenderer")
class ScalaClassRenderer extends ClassRenderer {

  import ScalaClassRenderer._

  override val getUniqueId: String = getClass.getSimpleName

  override def getName: String = DebuggerBundle.message("scala.class.renderer")

  override def clone(): ClassRenderer = super.clone()

  def isApplicableFor(tpe: Type): Boolean = tpe match {
    case ct: ClassType => isScalaSource(ct) && !isStringBuilder(ct)
    case _ => false
  }

  override def isEnabled: Boolean = true

  override def shouldDisplay(context: EvaluationContext, ref: ObjectReference, f: Field): Boolean =
    !ScalaSyntheticProvider.hasSpecialization(f, Some(ref.referenceType())) &&
      !isScalaSpecialField(f)

  override def calcLabel(descriptor: ValueDescriptor, context: EvaluationContext, labelListener: DescriptorLabelListener): String = {
    val renderer = NodeRendererSettings.getInstance().getToStringRenderer
    renderer.calcLabel(descriptor, context, labelListener)
  }

  override def isApplicable(tpe: Type): Boolean = isApplicableFor(tpe)

  final override def buildChildren(value: Value, builder: ChildrenBuilder, context: EvaluationContext): Unit =
    buildChildrenOrShowError(builder)(doBuildChildren(value, builder, context))

  // upstream: fields are collected asynchronously (buildChildrenAsync), consulo-java renderers are synchronous
  protected def doBuildChildren(value: Value, builder: ChildrenBuilder, context: EvaluationContext): Unit = {
    val ref = value.asInstanceOf[ObjectReference]
    val project = context.getProject
    val toShow = ref.referenceType().allFields().asScala.filter(shouldDisplay(context, ref, _))
    val nodeManager = builder.getNodeManager
    if (toShow.isEmpty) {
      builder.setChildren(Collections.singletonList(nodeManager.createMessageNode(JavaDebuggerLocalize.messageNodeClassNoFieldsToDisplay())))
    } else {
      val nodes = toShow.map { field =>
        nodeManager.createNode(fieldDescriptor(project, ref, field, context), context)
      }
      builder.setChildren(nodes.asJava)
    }
  }

  override def getChildValueExpression(node: DebuggerTreeNode, context: DebuggerContext): PsiElement =
    node.getDescriptor match {
      case _: FieldDescriptor => super.getChildValueExpression(node, context)
      case _ => null
    }
}

private object ScalaClassRenderer {
  private val LOG = Logger.getInstance(classOf[ScalaClassRenderer])

  private val Module: String = ScalaBytecodeConstants.ObjectSingletonInstanceName

  private val Bitmap: String = "bitmap$"

  private final val OffsetPrefix = "OFFSET$"

  private final val VarHandleSuffix = "$lzyHandle"

  private def isScalaSpecialField(f: Field): Boolean = {
    val name = f.name()
    name == Module || name.contains(Bitmap) || name.endsWith(VarHandleSuffix) || name.startsWith(OffsetPrefix)
  }

  private def fieldDescriptor(project: Project, ref: ObjectReference, field: Field, context: EvaluationContext): NodeDescriptor =
    if (!isLazyVal(ref, field)) new FieldDescriptorImpl(project, ref, field)
    else
      try LazyValDescriptor.create(project, ref, field, context.asInstanceOf[EvaluationContextImpl])
      catch {
        // A lazy val that is compiled in a way the renderer does not know must not hide the other fields
        case e: EvaluateException =>
          LOG.error(s"Cannot show the field ${field.name()} of ${ref.referenceType().name()} as a lazy val", e)
          new FieldDescriptorImpl(project, ref, field)
      }

  def isScalaSource(ct: ClassType): Boolean =
    Try(ct.sourceName().endsWith(".scala")).getOrElse(false)

  def isLazyVal(ref: ObjectReference, f: Field): Boolean = {
    val allFields = ref.referenceType().allFields().asScala
    val isScala3 = allFields.exists(f => isVarHandleOrOffsetField(f.name()))
    val fieldName = f.name()
    if (isScala3) fieldName.contains("$lzy")
    else {
      val allMethods = ref.referenceType().allMethods().asScala
      allMethods.exists { m =>
        val methodName = m.name()
        methodName.contains(s"$fieldName$$lzycompute")
      }
    }
  }

  private[ui] def isVarHandleOrOffsetField(fieldName: String): Boolean =
    fieldName.endsWith(VarHandleSuffix) || fieldName.startsWith(OffsetPrefix)

  private[ui] def isStringBuilder(ct: ClassType): Boolean =
    DebuggerUtils.instanceOf(ct, "scala.collection.mutable.StringBuilder")
}
