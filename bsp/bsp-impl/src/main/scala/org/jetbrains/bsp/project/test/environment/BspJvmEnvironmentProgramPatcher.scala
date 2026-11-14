package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.ExtensionImpl

import consulo.execution.executor.Executor
import com.intellij.java.execution.runners.JavaProgramPatcher
import consulo.execution.configuration.RunProfile
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.util.dataholder.UserDataHolderBase

import scala.jdk.CollectionConverters._

@ExtensionImpl
class BspJvmEnvironmentProgramPatcher extends JavaProgramPatcher {

  override def patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: OwnJavaParameters): Unit = {
    configuration match {
      case testConfig: UserDataHolderBase =>
        val env = testConfig.getUserData(BspFetchEnvironmentTask.jvmEnvironmentKey)
        if (env != null) {
          val oldEnvironmentVariables = javaParameters.getEnv.asScala.toMap
          val newEnvironmentVariables = oldEnvironmentVariables ++ env.environmentVariables
          javaParameters.setEnv(newEnvironmentVariables.asJava)

          val oldClasspath = javaParameters.getClassPath.getPathList.asScala.toList
          val newClassPath = env.classpath ++ oldClasspath
          javaParameters.getClassPath.clear()
          javaParameters.getClassPath.addAll(newClassPath.map(_.toString).asJava)

          javaParameters.setWorkingDirectory(env.workdir.toAbsolutePath.toString)
          javaParameters.getVMParametersList.addAll(env.jvmOptions.asJava)
        }
      case _ =>
    }
  }
}
