package org.jetbrains.plugins.scala
package debugger
package evaluation

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.module.Module
import consulo.project.Project
import consulo.util.io.FileUtil
import jakarta.inject.Singleton
import org.jetbrains.plugins.scala.api.NlsString

import java.nio.file.{Files, Path}
import scala.collection.mutable

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
class ScalaEvaluatorCompileHelper extends EvaluatorCompileHelper {

  private val tempFiles: mutable.Set[Path] = mutable.Set.empty

  private[evaluation] def clearTempFiles(): Unit = synchronized {
    tempFiles.foreach(FileUtil.delete)
    tempFiles.clear()
  }

  private def tempDir(): Path = synchronized {
    val dir = Files.createTempDirectory("classfilesForDebugger")
    tempFiles += dir
    dir
  }

  private def tempFile(): Path = synchronized {
    val file = Files.createTempFile("FileToCompile", ".scala")
    tempFiles += file
    file
  }

  // upstream: compiled by the Scala compile server
  override def compile(fileText: String, module: Module): Array[(Path, String)] = {
    val outputDir = tempDir()
    val sourceFile = tempFile()
    Files.writeString(sourceFile, fileText)
    try {
      EvaluatorCompiler.compile(module, Seq(sourceFile), outputDir) match {
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
}
