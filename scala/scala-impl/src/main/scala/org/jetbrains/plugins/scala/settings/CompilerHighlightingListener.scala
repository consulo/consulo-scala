package org.jetbrains.plugins.scala.settings

import consulo.annotation.component.{ComponentScope, TopicAPI}

@TopicAPI(ComponentScope.APPLICATION)
trait CompilerHighlightingListener {
  def compilerHighlightingScala2Changed(enabled: Boolean): Unit
  def compilerHighlightingScala3Changed(enabled: Boolean): Unit
}

object CompilerHighlightingListener {
  val Topic: Class[CompilerHighlightingListener] = classOf[CompilerHighlightingListener]
}
