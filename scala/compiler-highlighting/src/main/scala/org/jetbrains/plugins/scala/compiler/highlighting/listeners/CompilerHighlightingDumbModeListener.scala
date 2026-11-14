package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.project.Project
import consulo.project.event.DumbModeListener
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.compiler.highlighting.services.HighlightingService

// upstream: DumbModeListener (renamed, the Consulo topic has the same name)
@TopicImpl(ComponentScope.PROJECT)
final class CompilerHighlightingDumbModeListener @Inject()(project: Project) extends DumbModeListener:
  override def exitDumbMode(): Unit =
    if !project.isDisposed then HighlightingService(project).request("Exited dumb mode")
