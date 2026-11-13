package org.jetbrains.plugins.scala.runner

import consulo.compiler.util.CompilerUtil
import consulo.execution.event.ExecutionListener
import com.intellij.java.execution.impl.application.ApplicationConfiguration
import consulo.execution.runner.ExecutionEnvironment
import consulo.compiler.CompilerPaths
import org.jetbrains.plugins.scala.runner.ScalaApplicationConfigurationProducer.isScala3ApplicationConfiguration

import java.util

class SyncOutputExecutionListener extends ExecutionListener {
  override def processStarting(executorId: String, env: ExecutionEnvironment): Unit = {
    env.getRunProfile match {
      case app: ApplicationConfiguration if isScala3ApplicationConfiguration(app) =>
        val outputPaths = CompilerPaths.getOutputPaths(app.getModules)
        CompilerUtil.refreshOutputRoots(util.Arrays.asList(outputPaths*))
      case _ =>
    }
  }
}