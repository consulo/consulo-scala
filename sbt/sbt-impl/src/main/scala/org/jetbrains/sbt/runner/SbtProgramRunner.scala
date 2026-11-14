package org.jetbrains.sbt.runner

import consulo.annotation.component.ExtensionImpl

import consulo.execution.configuration.RunProfile
import consulo.execution.configuration.{RunProfileState, RunnerSettings}
import consulo.execution.runner.ExecutionEnvironment
import consulo.execution.runner.GenericProgramRunner
import consulo.execution.ui.RunContentDescriptor
/**
 * Handles SBT task run configurations only when they are delegated to the SBT shell.
 *
 * When [[SbtRunConfiguration.useSbtShell]] is disabled, this runner intentionally does not claim the configuration in [[canRun]].<br>
 * In that mode the standard platform run runner executes the [[SbtCommandLineState]] as a
 * [[com.intellij.execution.configurations.JavaCommandLineState]],
 * using the Java command line assembled by [[SbtCommandLineState.createJavaParameters]].
 *
 * @see [[org.jetbrains.sbt.runner.SbtDebugProgramRunner]]
 */
// upstream: order = "first"
@ExtensionImpl
class SbtProgramRunner extends GenericProgramRunner[RunnerSettings] with SbtProgramRunnerBase {

  override def getRunnerId: String = "SbtProgramRunner"

  override def canRun(executorId: String, profile: RunProfile): Boolean =
    isSbtRunConfigurationWithUseSbtShell(profile) && !isDebugExecutorId(executorId)

  override def doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor = {
    state match {
      case sbtState: SbtCommandLineState if sbtState.configuration.useSbtShell =>
        delegateExecutionToSbtShell(environment, sbtState)
      case _ =>
        super.doExecute(state, environment)
    }
  }
}
