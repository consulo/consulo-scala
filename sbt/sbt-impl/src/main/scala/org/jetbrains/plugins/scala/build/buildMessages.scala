package org.jetbrains.plugins.scala.build

import consulo.application.Application
import consulo.build.ui.event.{BuildEvent, BuildEventFactory, Failure, MessageEvent, Warning}
import consulo.build.ui.{BuildNotificationsGroups, FilePosition}
import consulo.localize.LocalizeValue
import consulo.navigation.Navigatable
import consulo.process.ProcessOutputTypes
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
    // upstream: event time and message group (kind name) are not supported by Consulo's BuildEventFactory
    val factory = Application.get().getInstance(classOf[BuildEventFactory])
    val text = LocalizeValue.of(stripAnsiCodes(message))
    val description = LocalizeValue.ofNullable(details)

    position match {
      case None =>
        factory.createMessageEvent(parentId, kind, BuildNotificationsGroups.BUILD_ISSUES, text, description, navigatable.orNull)
      case Some(filePosition) =>
        factory.createFileMessageEvent(parentId, kind, BuildNotificationsGroups.BUILD_ISSUES, text, description, filePosition)
    }
  }
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