package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.{ComponentScope, ServiceAPI}
import consulo.disposer.Disposable
import consulo.project.Project
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.{CompilationRequest, HighlightingRequest}

@ServiceAPI(ComponentScope.PROJECT)
trait HighlightingService extends Disposable:

  /** Can be used to notify that we need to trigger highlighting */
  def request(reason: String): Unit

  /** Records that `request`'s file wants highlighting */
  def request(request: HighlightingRequest): Unit

  /**
   * Executes `request` right now, on the calling thread, bypassing the scheduler: no waiting, no
   * planning, no deciding. Failures propagate to the caller.
   */
  def compile(request: CompilationRequest): Unit

object HighlightingService:
  def apply(project: Project): HighlightingService =
    project.getInstance(classOf[HighlightingService])
