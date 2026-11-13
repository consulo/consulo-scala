package org.jetbrains.plugins.scala.actions.utils

import consulo.annotation.component.{ComponentScope, TopicAPI}
import consulo.ui.ex.action.AnAction
import org.jetbrains.annotations.TestOnly

import scala.util.Try

/**
 * The current primary purpose of this listener is to make asynchronous actions testable
 */
@TestOnly
@TopicAPI(ComponentScope.PROJECT)
private[actions]
trait ScalaAsyncActionListener {
  /**
   * @param actionClass The class of the action that was completed
   * @param result      Success if the action was completed successfully<br>
   *                    Failure(exception) if the action failed with the given exception (on any thread)
   */
  def actionCompleted(actionClass: Class[? <: AnAction], result: Try[Unit]): Unit
}

object ScalaAsyncActionListener {
  val Topic: Class[ScalaAsyncActionListener] = classOf[ScalaAsyncActionListener]
}
