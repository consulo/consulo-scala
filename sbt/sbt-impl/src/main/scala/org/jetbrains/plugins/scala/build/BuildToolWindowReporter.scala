package org.jetbrains.plugins.scala.build

import consulo.platform.base.icon.PlatformIconGroup

import consulo.application.{AllIcons, Application}
import consulo.application.progress.ProgressIndicator
import consulo.build.ui.event.{BuildEvent, BuildEventFactory, EventResult, MessageEvent, ProgressBuildEvent}
import consulo.build.ui.issue.BuildIssue
import consulo.build.ui.progress.BuildProgressListener
import consulo.build.ui.{BuildContentManager, BuildViewManager, DefaultBuildDescriptor, FilePosition}
import consulo.component.ProcessCanceledException
import consulo.execution.ui.RunContentDescriptor
import consulo.localize.LocalizeValue
import consulo.navigation.Navigatable
import consulo.project.Project
import consulo.project.ui.wm.ToolWindowManager
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate, DumbAwareAction}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.build.BuildMessages.EventId
import org.jetbrains.sbt.SbtBundle

import java.nio.file.Path
import javax.swing.JComponent
import scala.concurrent.Promise

/**
 * Reports events to the Build Tool Window.
 *
 * @param activateToolWindowWhenFailed If true, activates the tool window when a finish event with failure result is emitted.
 * @param activateToolWindowWhenWarned If true, activates the tool window when the first warning is emitted.
 */
class BuildToolWindowReporter(
  project: Project,
  buildId: EventId,
  @Nls title: String,
  viewManager: BuildProgressListener,
  cancelAction: AnAction,
  activateToolWindowWhenFailed: Boolean,
  activateToolWindowWhenWarned: Boolean
) extends BuildReporter {

  import MessageEvent.Kind

  private var warningActivatedToolWindow = false

  private def factory: BuildEventFactory = Application.get().getInstance(classOf[BuildEventFactory])

  def this(
    project: Project,
    buildId: EventId,
    @Nls title: String,
    cancelAction: AnAction,
    activateToolWindowWhenFailed: Boolean = true,
    activateToolWindowWhenWarned: Boolean = false
  ) =
    this(
      project,
      buildId,
      title,
      project.getInstance(classOf[BuildViewManager]),
      cancelAction,
      activateToolWindowWhenFailed = activateToolWindowWhenFailed,
      activateToolWindowWhenWarned = activateToolWindowWhenWarned
    )

  override def start(): Unit = {
    val buildDescriptor =
      new DefaultBuildDescriptor(buildId, LocalizeValue.of(title), project.getBasePath, System.currentTimeMillis())
        .withContentDescriptor { () => // dummy runContentDescriptor to set autofocus of build toolwindow off
          val descriptor = new RunContentDescriptor(null, null, new JComponent {}, title)
          descriptor.setActivateToolWindowWhenAdded(false)
          descriptor.setAutoFocusContent(false)
          descriptor
        }
        .withRestartActions(cancelAction)

    buildDescriptor.setActivateToolWindowWhenFailed(activateToolWindowWhenFailed)
    val startEvent = factory.createStartBuildEvent(buildDescriptor, LocalizeValue.of(SbtBundle.message("report.build.toolwindow.running")))
    viewManager.onEvent(buildId, startEvent)
  }

  override def finish(messages: BuildMessages): Unit = {
    @Nls
    val (result, resultMessage) =
      if (messages.status == BuildMessages.OK && messages.errors.isEmpty)
        (factory.createSuccessResult(), "success")
      else if (messages.status == BuildMessages.Canceled)
        (factory.createSkippedResult(), "canceled")
      else {
        (factory.createFailureResult(null: Throwable), "failed")
      }

    val finishEvent = finishBuildEvent(resultMessage, result)
    viewManager.onEvent(buildId, finishEvent)
  }

  override def finishWithFailure(err: Throwable): Unit = {
    val finishEvent = finishBuildEvent(SbtBundle.message("report.build.toolwindow.failed"), factory.createFailureResult(err))
    viewManager.onEvent(buildId, finishEvent)
  }

  override def finishCanceled(): Unit = {
    val finishEvent = finishBuildEvent(SbtBundle.message("report.build.toolwindow.canceled"), factory.createSkippedResult())
    viewManager.onEvent(buildId, finishEvent)
  }

  private def finishBuildEvent(@Nls message: String, result: EventResult): BuildEvent =
    factory.createFinishBuildEvent(buildId, null, System.currentTimeMillis(), LocalizeValue.of(message), result)

  override def startTask(taskId: EventId, parent: Option[EventId], message: String, time: Long = System.currentTimeMillis()): Unit = {
    val startEvent = factory.createStartEvent(taskId, parent.orNull, time, LocalizeValue.of(message))
    viewManager.onEvent(buildId, startEvent)
  }

  override def progressTask(taskId: EventId, total: Long, progress: Long, unit: String, message: String, time: Long = System.currentTimeMillis()): Unit = {
    val event = BuildToolWindowReporter.ProgressEvent(
      taskId, time, LocalizeValue.of(message), total, progress,
      if (unit == null) SbtBundle.message("report.build.toolwindow.items") else unit
    )
    viewManager.onEvent(buildId, event)
  }

  override def finishTask(taskId: EventId, message: String, result: EventResult, time: Long = System.currentTimeMillis()): Unit = {
    val event = factory.createFinishEvent(taskId, null, time, LocalizeValue.of(message), result)
    viewManager.onEvent(buildId, event)
  }

  override def clear(file: Path): Unit = ()

  override def warning(message: String, position: Option[FilePosition]): Unit = {
    activateToolWindowOnWarning()
    viewManager.onEvent(buildId, event(message, Kind.WARNING, position))
  }

  override def warning(message: String, position: Option[FilePosition], details: String): Unit = {
    activateToolWindowOnWarning()
    viewManager.onEvent(buildId, event(message, Kind.WARNING, position, details = details))
  }

  override def warning(issue: BuildIssue): Unit = {
    activateToolWindowOnWarning()
    val event = factory.createBuildIssueEvent(buildId, issue, Kind.WARNING)
    viewManager.onEvent(buildId, event)
  }

  override def warning(message: String, position: Option[FilePosition], details: String, navigatable: Option[Navigatable]): Unit = {
    activateToolWindowOnWarning()
    viewManager.onEvent(buildId, event(message, Kind.WARNING, position, details = details, navigatable = navigatable))
  }

  override def error(message: String, position: Option[FilePosition]): Unit =
    viewManager.onEvent(buildId, event(message, Kind.ERROR, position))

  override def info(message: String, position: Option[FilePosition]): Unit =
    viewManager.onEvent(buildId, event(message, Kind.INFO, position))

  override def log(message: String): Unit =
    viewManager.onEvent(buildId, logEvent(message, isStdout = true))

  override def logErr(message: String): Unit =
    viewManager.onEvent(buildId, logEvent(message, isStdout = false))

  private def logEvent(msg: String, isStdout: Boolean): BuildEvent =
    factory.createOutputBuildEvent(buildId, msg.trim + System.lineSeparator(), isStdout)

  private def event(
    message: String,
    kind: MessageEvent.Kind,
    position: Option[FilePosition],
    details: String = null,
    navigatable: Option[Navigatable] = None
  ) = {
    //noinspection ReferencePassedToNls
    BuildMessages.message(buildId, message, kind, position, eventTime = System.currentTimeMillis, details, navigatable)
  }

  private def activateToolWindowOnWarning(): Unit =
    if (activateToolWindowWhenWarned && !warningActivatedToolWindow) {
      warningActivatedToolWindow = true
      ToolWindowManager.getInstance(project).invokeLater { () =>
        BuildContentManager.getInstance(project).getOrCreateToolWindow.activate(null, true)
      }
    }
}

object BuildToolWindowReporter {

  private case class ProgressEvent(
    id: Any,
    eventTime: Long,
    message: LocalizeValue,
    total: Long,
    progress: Long,
    unit: String
  ) extends ProgressBuildEvent {
    override def getId: AnyRef = id.asInstanceOf[AnyRef]
    override def getParentId: AnyRef = null
    override def getEventTime: Long = eventTime
    override def getMessage: LocalizeValue = message
    override def getDescription: LocalizeValue = LocalizeValue.empty()
    override def getHint: LocalizeValue = LocalizeValue.empty()
    override def getTotal: Long = total
    override def getProgress: Long = progress
    override def getUnit: String = unit
  }

  /** Action to cancel an ongoing build by failing the promise and canceling the progress indicator. */
  class CancelBuildAction(cancelToken: Promise[?], indicator: Option[ProgressIndicator])
    extends DumbAwareAction(LocalizeValue.of(SbtBundle.message("report.build.toolwindow.cancel")), LocalizeValue.of(SbtBundle.message("report.build.toolwindow.cancel")), PlatformIconGroup.actionsSuspend()) with AnActionWithSyncUpdate {

    override def actionPerformed(e: AnActionEvent): Unit = {
      cancelToken.failure(new ProcessCanceledException())
      indicator.foreach(_.cancel())
    }

    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(!cancelToken.isCompleted)

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }
}
