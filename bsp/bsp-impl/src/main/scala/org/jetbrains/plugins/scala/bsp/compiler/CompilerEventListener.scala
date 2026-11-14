package org.jetbrains.plugins.scala.bsp.compiler

import consulo.annotation.component.{ComponentScope, TopicAPI}
import org.jetbrains.plugins.scala.shared.compiler.CompilerEvent

import java.util.EventListener

@TopicAPI(ComponentScope.PROJECT)
trait CompilerEventListener
  extends EventListener {

  def eventReceived(event: CompilerEvent): Unit
}

object CompilerEventListener {

  final val topic: Class[CompilerEventListener] = classOf[CompilerEventListener]
}
