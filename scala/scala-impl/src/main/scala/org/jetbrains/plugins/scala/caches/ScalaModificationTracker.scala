package org.jetbrains.plugins.scala.caches

import consulo.component.util.SimpleModificationTracker
import org.jetbrains.plugins.scala.Tracing

class ScalaModificationTracker(name: String) extends SimpleModificationTracker {
  override def incModificationCount(): Unit = {
    super.incModificationCount()

    Tracing.modification(name, getModificationCount)
  }
}
