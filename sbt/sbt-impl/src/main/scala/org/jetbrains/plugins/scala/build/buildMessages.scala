package org.jetbrains.plugins.scala.build

import consulo.application.Application
import consulo.build.ui.event.{BuildEvent, BuildEventFactory, Failure, FileMessageEvent, FileMessageEventResult, MessageEvent, MessageEventResult, ProgressBuildEvent, Warning}
import consulo.build.ui.{BuildNotificationsGroups, FilePosition}
import consulo.localize.LocalizeValue
import consulo.navigation.Navigatable
import consulo.process.ProcessOutputTypes
import consulo.project.Project
import consulo.project.ui.notification.NotificationGroup
import consulo.process.util.AnsiEscapeDecoder
import org.jetbrains.annotations.{Nls, Nullable}
import org.jetbrains.plugins.scala.build.BuildMessages.{BuildStatus, Canceled, Error}

import java.util.UUID

case class BuildMessages(
  warnings: Seq[Warning],
  errors: Seq[Failure],
  exceptions: Seq[Exception],
  messages: Seq[String],
  status: BuildStatus
) {
  def addError(msg: String): BuildMessages = copy(errors = errors :+ BuildFailure(msg.trim))

  def addWarning(msg: String): BuildMessages = copy(warnings = warnings :+ BuildWarning(msg.trim))

  def status(buildStatus: BuildStatus): BuildMessages = copy(status = buildStatus)

  def exception(exception: Exception): BuildMessages = copy(exceptions = exceptions :+ exception, status = Error)

  def message(msg: String): BuildMessages = copy(messages = messages :+ msg)

  def combine(other: BuildMessages): BuildMessages = BuildMessages(
    this.warnings ++ other.warnings,
    this.errors ++ other.errors,
    this.exceptions ++ other.exceptions,
    this.messages ++ other.messages,
    this.status.combine(other.status)
  )

  def toTaskRunnerResult: TaskRunnerResult = {
    TaskRunnerResult(
      status == Canceled,
      status == Error || errors.nonEmpty
    )
  }
}

//noinspection ApiStatus,UnstableApiUsage
case object BuildMessages {

  sealed abstract class BuildStatus {
    def combine(other: BuildStatus): BuildStatus = (this, other) match {
      case (Indeterminate, s) => s
      case (s, Indeterminate) => s
      case (Error, _) | (_, Error) => Error
      case (Canceled, _) | (_, Canceled) => Canceled
      case (OK, OK) => OK
    }
  }

  case object Indeterminate extends BuildStatus
  case object OK extends BuildStatus
  case object Error extends BuildStatus
  case object Canceled extends BuildStatus


  case class EventId(id: String) extends AnyVal

  def randomEventId: EventId = EventId(UUID.randomUUID().toString)

  def empty: BuildMessages = BuildMessages(Vector.empty, Vector.empty, Vector.empty, Vector.empty, BuildMessages.Indeterminate)

  /**
   * Strips ANSI escape codes from the given message.
   */
  def stripAnsiCodes(@Nullable message: String): String =
    if (message == null) null
    else {
      val builder = new StringBuilder()
      new AnsiEscapeDecoder().escapeText(message, ProcessOutputTypes.STDOUT, (text, _) => builder.append(text))
      builder.result()
    }

  def message(
    parentId: Any,
    @Nls message: String,
    kind: MessageEvent.Kind,
    position: Option[FilePosition],
    eventTime: Long,
    @Nls @Nullable details: String = null,
    navigatable: Option[Navigatable] = None,
  ): BuildEvent = {
    // upstream: the message group is the kind name - Consulo's message groups are notification groups which the build view
    // does not group by, all messages use the build issues group
    val factory = Application.get().getInstance(classOf[BuildEventFactory])
    val text = LocalizeValue.of(stripAnsiCodes(message))
    val description = LocalizeValue.ofNullable(details)

    position match {
      case None =>
        new TimedMessageEvent(factory.createMessageEvent(parentId, kind, BuildNotificationsGroups.BUILD_ISSUES, text, description, navigatable.orNull), eventTime)
      case Some(filePosition) =>
        new TimedFileMessageEvent(factory.createFileMessageEvent(parentId, kind, BuildNotificationsGroups.BUILD_ISSUES, text, description, filePosition), eventTime)
    }
  }

  def progress(eventId: Any, parentId: Any, eventTime: Long, @Nls message: String, total: Long, progress: Long, unit: String): BuildEvent =
    new SimpleProgressBuildEvent(eventId, parentId, eventTime, LocalizeValue.of(message), total, progress, unit)
}

// upstream: BuildEvents builders take the event time (withTime), Consulo's BuildEventFactory always uses the current time
private class TimedMessageEvent(delegate: MessageEvent, eventTime: Long) extends MessageEvent {
  override def getId: AnyRef = delegate.getId

  override def getParentId: AnyRef = delegate.getParentId

  override def getEventTime: Long = eventTime

  override def getMessage: LocalizeValue = delegate.getMessage

  override def getDescription: LocalizeValue = delegate.getDescription

  override def getHint: LocalizeValue = delegate.getHint

  override def getKind: MessageEvent.Kind = delegate.getKind

  override def getGroup: NotificationGroup = delegate.getGroup

  override def getNavigatable(project: Project): Navigatable = delegate.getNavigatable(project)

  override def getResult: MessageEventResult = delegate.getResult

  override def equals(obj: Any): Boolean = obj match {
    case other: TimedMessageEvent => delegate == other.delegateEvent
    case _ => false
  }

  override def hashCode(): Int = delegate.hashCode()

  protected def delegateEvent: MessageEvent = delegate
}

private final class TimedFileMessageEvent(delegate: FileMessageEvent, eventTime: Long)
  extends TimedMessageEvent(delegate, eventTime) with FileMessageEvent {

  override def getFilePosition: FilePosition = delegate.getFilePosition

  override def getResult: FileMessageEventResult = delegate.getResult
}

// upstream: BuildEvents.progress
private final class SimpleProgressBuildEvent(
  eventId: Any,
  parentId: Any,
  eventTime: Long,
  message: LocalizeValue,
  total: Long,
  progress: Long,
  unit: String
) extends ProgressBuildEvent {
  override def getId: AnyRef = eventId.asInstanceOf[AnyRef]

  override def getParentId: AnyRef = parentId.asInstanceOf[AnyRef]

  override def getEventTime: Long = eventTime

  override def getMessage: LocalizeValue = message

  override def getDescription: LocalizeValue = LocalizeValue.empty()

  override def getHint: LocalizeValue = LocalizeValue.empty()

  override def getTotal: Long = total

  override def getProgress: Long = progress

  override def getUnit: String = unit
}

case class TaskRunnerResult(
  isAborted: Boolean,
  hasErrors: Boolean
)

case class TaskManagerResult(
  isAborted: Boolean,
  hasErrors: Boolean
)

case class BuildFailure(@Nls message: String) extends Failure {
  override def getMessage: LocalizeValue = LocalizeValue.of(message)

  override def getDescription: LocalizeValue = LocalizeValue.empty()

  override def getCauses: java.util.List[? <: Failure] = java.util.Collections.emptyList()
}

case class BuildWarning(@Nls message: String) extends Warning {
  override def getMessage: String = message

  override def getDescription: String = null
}