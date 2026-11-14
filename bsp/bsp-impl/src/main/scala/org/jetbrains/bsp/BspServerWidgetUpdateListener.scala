package org.jetbrains.bsp

import consulo.annotation.component.{ComponentScope, TopicAPI}

// upstream: BspServerWidgetFactory.UpdateWidgetListener (nested), topics are top level interfaces in Consulo
@TopicAPI(ComponentScope.APPLICATION)
trait BspServerWidgetUpdateListener {
  def updateWidget(): Unit
}
