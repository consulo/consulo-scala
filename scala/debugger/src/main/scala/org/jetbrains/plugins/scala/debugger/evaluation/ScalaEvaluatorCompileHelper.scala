package org.jetbrains.plugins.scala
package debugger
package evaluation

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Inject
import jakarta.inject.Singleton


import org.jetbrains.plugins.scala.api.NlsString
import com.intellij.java.debugger.impl.{DebuggerManagerListener, DebuggerSession}
import consulo.module.Module
import consulo.project.Project
import com.intellij.openapi.util.io.NioFiles
import com.intellij.platform.eel.provider.utils.EelProjectUtils
import org.jetbrains.plugins.scala.server.compiler.CompileServerLauncher
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.concurrent.duration.Duration

//noinspection ApiStatus,UnstableApiUsage
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
class ScalaEvaluatorCompileHelper @Inject()(project: Project) extends EvaluatorCompileHelper {

  private val tempFiles: mutable.Set[Path] = mutable.Set.empty

  private def clearTempFiles(): Unit = {
    tempFiles.foreach(NioFiles.deleteRecursively)
    tempFiles.clear()
  }

  private def tempDir(): Path = {
    val dir = EelProjectUtils.createTemporaryDirectory(project, "classfilesForDebugger", "", true)
    tempFiles += dir
    dir
  }

  private def tempFile(): Path = {
    val file = EelProjectUtils.createTemporaryFile(project, "FileToCompile", ".scala", true)
    tempFiles += file
    file
  }

  override def compile(fileText: String, module: Module): Array[(Path, String)] = {
    if (EvaluatorCompileHelper.needCompileServer) {
      CompileServerLauncher.ensureServerRunning(project)
    }
    val outputDir = tempDir()
    val sourceFile = tempFile()
    Files.writeString(sourceFile, fileText)
    val connector = new ServerConnector(module, Seq(sourceFile), outputDir)
    try {
      connector.compile() match {
        case Right(output) => output
        case Left(errors) => throw EvaluationException(NlsString.force(errors.mkString("\n")))
      }
    }
    catch {
      case e: Exception => throw EvaluationException(DebuggerBundle.message("could.not.compile", e.getMessage))
    }
  }
}

object ScalaEvaluatorCompileHelper {
  def instance(project: Project): ScalaEvaluatorCompileHelper =
    project.getInstance(classOf[ScalaEvaluatorCompileHelper])

  private class Listener(project: Project) extends DebuggerManagerListener {

    override def sessionDetached(session: DebuggerSession): Unit = {
      instance(project).clearTempFiles()

      if (!ScalaCompileServerSettings.getInstance().COMPILE_SERVER_ENABLED && EvaluatorCompileHelper.needCompileServer) {
        CompileServerLauncher.stopServerAndWaitFor(Duration.Zero)
      }
    }
  }
}
