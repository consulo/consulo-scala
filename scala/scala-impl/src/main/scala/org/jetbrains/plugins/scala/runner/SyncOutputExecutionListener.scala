package org.jetbrains.plugins.scala.runner

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.TopicImpl

import consulo.compiler.util.CompilerUtil
import consulo.execution.event.ExecutionListener
import com.intellij.java.execution.impl.application.ApplicationConfiguration
import consulo.execution.runner.ExecutionEnvironment
import consulo.compiler.CompilerPaths
import org.jetbrains.plugins.scala.runner.ScalaApplicationConfigurationProducer.isScala3ApplicationConfiguration

import java.nio.file.Path
import java.util

@TopicImpl(ComponentScope.PROJECT)
class SyncOutputExecutionListener extends ExecutionListener {
  override def processStarting(executorId: String, env: ExecutionEnvironment): Unit = {
    env.getRunProfile match {
      case app: ApplicationConfiguration if isScala3ApplicationConfiguration(app) =>
        val outputPaths = CompilerPaths.getOutputPaths(app.getModules)
        // upstream: CompilerUtil.refreshOutputRoots
        CompilerUtil.refreshDirectories(util.Arrays.asList(outputPaths.map(Path.of(_))*))
      case _ =>
    }
  }
}