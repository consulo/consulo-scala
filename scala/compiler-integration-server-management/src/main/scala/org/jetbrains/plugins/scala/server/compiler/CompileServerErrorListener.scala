package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.{ComponentScope, TopicAPI}

@TopicAPI(ComponentScope.APPLICATION)
trait CompileServerErrorListener {
  def onError(text: String): Unit
}

object CompileServerErrorListener {
  val Topic: Class[CompileServerErrorListener] = classOf[CompileServerErrorListener]
}
