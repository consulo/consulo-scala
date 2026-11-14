package org.jetbrains.sbt.shell.communication


/**
 * Event emitted while a command submitted to the sbt shell is being processed.
 */
// upstream: @Experimental
enum ShellEvent {
  case TaskStart
  case TaskComplete
  case ProcessTerminated
  case ErrorWaitForInput
  case Output(line: String)
}
