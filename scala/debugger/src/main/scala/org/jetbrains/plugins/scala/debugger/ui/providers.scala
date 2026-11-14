package org.jetbrains.plugins.scala.debugger.ui

import consulo.annotation.component.ExtensionImpl
//import com.intellij.debugger.collections.visualizer.CollectionVisualizerEvaluator
//import com.intellij.debugger.engine.FullValueEvaluatorProvider
//import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.DebuggerUtilsAsync
//import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.java.debugger.impl.ui.tree.render.{ChildrenRenderer, CompoundRendererProvider, ValueLabelRenderer}
import consulo.internal.com.sun.jdi.Type

import java.util.concurrent.CompletableFuture

abstract class ScalaRendererProvider(private val renderer: ScalaClassRenderer) extends CompoundRendererProvider {

  override def getName: String = renderer.getName

  override def getValueLabelRenderer: ValueLabelRenderer = renderer

  override def getChildrenRenderer: ChildrenRenderer = renderer

  override def isEnabled: Boolean = renderer.isEnabled

  override def getIsApplicableChecker: java.util.function.Function[Type, CompletableFuture[java.lang.Boolean]] =
    tpe => DebuggerUtilsAsync.reschedule(CompletableFuture.supplyAsync(() => renderer.isApplicableFor(tpe)))
}

@ExtensionImpl(id = "scalaClassRendererProvider")
class ScalaClassRendererProvider extends ScalaRendererProvider(new ScalaClassRenderer())

@ExtensionImpl(id = "scalaCollectionRendererProvider", order = "before scalaClassRendererProvider")
class ScalaCollectionRendererProvider extends ScalaRendererProvider(new ScalaCollectionRenderer())
/*{
  override def getFullValueEvaluatorProvider: FullValueEvaluatorProvider =
    (evaluationContext: EvaluationContextImpl, valueDescriptor: ValueDescriptorImpl) =>
      CollectionVisualizerEvaluator.createFor(evaluationContext, valueDescriptor)
}*/
