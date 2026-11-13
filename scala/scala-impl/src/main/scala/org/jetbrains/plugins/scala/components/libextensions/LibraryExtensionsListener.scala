package org.jetbrains.plugins.scala.components.libextensions

import consulo.annotation.component.{ComponentScope, TopicAPI}
@TopicAPI(ComponentScope.PROJECT)
trait LibraryExtensionsListener {
  def newExtensionsAdded(): Unit
}
