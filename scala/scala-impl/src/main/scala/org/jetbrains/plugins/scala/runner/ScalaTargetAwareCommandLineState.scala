package org.jetbrains.plugins.scala.runner

import com.intellij.java.execution.configurations.JavaCommandLineState
import consulo.execution.process.ProcessTerminatedListener
import consulo.execution.runner.ExecutionEnvironment
import consulo.process.{ProcessHandler, ProcessHandlerBuilder}

// upstream: @ApiStatus.Internal
abstract class ScalaTargetAwareCommandLineState(env: ExecutionEnvironment) extends JavaCommandLineState(env) {

  /**
   * @note This is a simplified version of `JavaTestFrameworkRunnableState#createHandler`.
   *       upstream: prepares the target environment (eel/WSL) and runs the targeted command line there;
   *       in Consulo the command line is created for the platform of the project.
   */
  protected def createHandler(): ProcessHandler = {
    val processHandler = ProcessHandlerBuilder.create(createCommandLine())
      .killable()
      .colored()
      .silentReader()
      .build()

    ProcessTerminatedListener.attach(processHandler)
    processHandler
  }
}
