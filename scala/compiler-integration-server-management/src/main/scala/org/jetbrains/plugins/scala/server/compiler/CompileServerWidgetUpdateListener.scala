package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.{ComponentScope, TopicAPI}

@TopicAPI(ComponentScope.APPLICATION)
trait CompileServerWidgetUpdateListener {
  def updateWidget(): Unit
}

object CompileServerWidgetUpdateListener {
  val Topic: Class[CompileServerWidgetUpdateListener] = classOf[CompileServerWidgetUpdateListener]
}
