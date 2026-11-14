package org.jetbrains.plugins.scala.build

import consulo.application.Application
import consulo.build.ui.event.MessageEvent.Kind
import consulo.build.ui.event.{BuildEvent, BuildEventFactory, EventResult, MessageEvent}
import consulo.build.ui.issue.BuildIssue
import consulo.build.ui.{BuildContentManager, FilePosition, SyncViewManager}
import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskNotificationEvent, ExternalSystemTaskNotificationListener}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.navigation.Navigatable
import consulo.project.ui.wm.ToolWindowManager
import org.jetbrains.annotations.{Nls, Nullable}
import org.jetbrains.plugins.scala.build.BuildMessages.EventId
import org.jetbrains.plugins.scala.build.ExternalSystemNotificationReporter.*
import org.jetbrains.sbt.SbtBundle

import java.nio.file.Path
import scala.collection.mutable

class ExternalSystemNotificationReporter(
  workingDir: String,
  taskId: ExternalSystemTaskId,
  notifications: ExternalSystemTaskNotificationListener
) extends BuildReporter {

  // upstream: workingDir is passed to the listener; Consulo's ExternalSystemTaskNotificationListener has no build events,
  // task events are sent to the sync view directly
  private val descriptors: mutable.Map[EventId, TaskDescriptor] = mutable.Map.empty

  private def factory: BuildEventFactory = Application.get().getInstance(classOf[BuildEventFactory])

  private val viewManager: Option[SyncViewManager] =
    Option(taskId.findProject()).map(_.getInstance(classOf[SyncViewManager]))

  private var isFinished: Boolean = false
  private var warningActivatedToolWindow: Boolean = false

  override def start(): Unit = {
    notifications.onStart(taskId)
  }

  override def finish(messages: BuildMessages): Unit = {
    isFinished = true
    if (messages.status == BuildMessages.OK && messages.errors.isEmpty) {
      notifications.onSuccess(taskId)
      notifications.onEnd(taskId)
    }
    else if (messages.status == BuildMessages.Canceled) {
      finishCanceled()
    }
    else if (messages.exceptions.nonEmpty) {
      finishWithFailure(messages.exceptions.head)
    } else if (messages.errors.nonEmpty) {
      val firstError = messages.errors.head
      val throwable = Option(firstError.getError).getOrElse(new Exception(firstError.getMessage.get))
      finishWithFailure(throwable)
    } else if (messages.status == BuildMessages.Indeterminate) {
      finishWithFailure(new Exception("task ended in indeterminate state"))
    } else {
      finishWithFailure(new Exception("task failed"))
    }
  }

  override def finishWithFailure(err: Throwable): Unit = {
    isFinished = true
    notifications.onFailure(taskId, throwableToException(err))
    notifications.onEnd(taskId)
  }

  override def finishCanceled(): Unit = {
    isFinished = true
    val time = System.currentTimeMillis()
    descriptors.foreach { case (id, _) =>
      finishTask(id, SbtBundle.message("report.build.task.canceled"), factory.createSkippedResult(), time)
    }
    // upstream: notifications.onCancel
    notifications.onEnd(taskId)
  }

  override def warning(message: String, position: Option[FilePosition]): Unit =
    onWarningEvent(message, position)

  override def warning(message: String, position: Option[FilePosition], details: String): Unit =
    onWarningEvent(message, position, details)

  override def warning(issue: BuildIssue): Unit =
    onWarningEvent(issue)

  override def warning(message: String, position: Option[FilePosition], details: String, navigatable: Option[Navigatable]): Unit =
    onWarningEvent(message, position, details, navigatable = navigatable)

  override def error(message: String, position: Option[FilePosition]): Unit =
    onEvent(message, Kind.ERROR, position)

  override def info(message: String, position: Option[FilePosition]): Unit =
    onEvent(message, Kind.INFO, position)

  override def info(issue: BuildIssue): Unit =
    onEvent(issue, Kind.INFO)

  private def onWarningEvent(
    @Nls message: String,
    position: Option[FilePosition],
    @Nls @Nullable details: String = null,
    navigatable: Option[Navigatable] = None
  ): Unit = {
    activateToolWindowOnWarning()
    onEvent(message, Kind.WARNING, position, details, navigatable)
  }

  private def onWarningEvent(issue: BuildIssue): Unit = {
    activateToolWindowOnWarning()
    onEvent(issue, Kind.WARNING)
  }

  private def onEvent(
    @Nls message: String,
    kind: Kind,
    position: Option[FilePosition],
    @Nls @Nullable details: String = null,
    navigatable: Option[Navigatable] = None
  ): Unit = {
    val event = createBuildEvent(message, kind, position, details, navigatable)
    onBuildEvent(event)
  }

  private def onEvent(issue: BuildIssue, kind: Kind): Unit = {
    val event = factory.createBuildIssueEvent(taskId, issue, kind)
    onBuildEvent(event)
  }

  private def onBuildEvent(event: BuildEvent): Unit =
    viewManager.foreach { manager =>
      manager.onEvent(taskId, event)
    }

  private def activateToolWindowOnWarning(): Unit =
    if (!warningActivatedToolWindow) {
      warningActivatedToolWindow = true
      Option(taskId.findProject()).foreach { project =>
        ToolWindowManager.getInstance(project).invokeLater { () =>
          BuildContentManager.getInstance(project).getOrCreateToolWindow.activate(null, true)
        }
      }
    }

  override def log(message: String): Unit =
    log(message, isStdOut = true)

  override def logErr(message: String): Unit =
    log(message, isStdOut = false)

  private def log(message: String, isStdOut: Boolean): Unit = synchronized {
    if (!isFinished) {
      //NOTE: new lines are also added in BspSession.BspProcessMessageHandler.call
      val messageWithNewLine = if (message.endsWith("\n")) message else message + "\n"
      notifications.onTaskOutput(taskId, messageWithNewLine, isStdOut)
    } else {
      //NOTE: it might be not valid to log output when the task is already finished
      //(see comments of https://youtrack.jetbrains.com/issue/SCL-21794/Reload-ALL-BSP-projects-action-is-disabled)
      Log.warn(s"Skipping output for a finished task (taskId: $taskId, stdout: $isStdOut): ${message.trim}")
    }
  }

  override def startTask(eventId: EventId, parent: Option[EventId], message: String, time: Long = System.currentTimeMillis()): Unit = {
    descriptors.getOrElseUpdate(eventId, TaskDescriptor(message, parent))
    notifications.onStatusChange(new ExternalSystemTaskNotificationEvent(taskId, message))
    onBuildEvent(factory.createStartEvent(eventId.id, parent.map(_.id).getOrElse(taskId), time, LocalizeValue.of(message)))
  }

  override def progressTask(eventId: EventId, total: Long, progress: Long, unit: String, message: String, time: Long = System.currentTimeMillis()): Unit = {
    descriptors.get(eventId).foreach { _ =>
      notifications.onStatusChange(new ExternalSystemTaskNotificationEvent(taskId, message))
    }
  }

  override def finishTask(eventId: EventId, message: String, result: EventResult, time: Long = System.currentTimeMillis()): Unit = {
    descriptors
      .remove(eventId)
      .foreach { taskDescriptor =>
        onBuildEvent(factory.createFinishEvent(eventId.id, taskDescriptor.parent.map(_.id).getOrElse(taskId), time, LocalizeValue.of(message), result))
      }
  }

  override def clear(file: Path): Unit = ()

  private def createBuildEvent(
    @Nls message: String,
    kind: MessageEvent.Kind,
    position: Option[FilePosition],
    @Nls @Nullable details: String,
    navigatable: Option[Navigatable] = None
  ): BuildEvent =
    BuildMessages.message(taskId, message, kind, position, eventTime = System.currentTimeMillis, details, navigatable)
}

object ExternalSystemNotificationReporter {
  private val Log = Logger.getInstance(this.getClass)

  private case class TaskDescriptor(@Nls message: String, parent: Option[EventId])

  private def throwableToException(throwable: Throwable) = throwable match {
    case x: Exception => x
    case t: Throwable => new Exception(t)
  }
}
