package org.jetbrains.plugins.scala
package worksheet.processor

import com.intellij.java.execution.impl.util.JavaParametersUtil
import consulo.execution.process.ProcessTerminatedListener
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.module.Module
import consulo.process.event.{ProcessEvent, ProcessListener}
import consulo.process.{ProcessHandler, ProcessHandlerBuilder, ProcessOutputType}
import consulo.util.dataholder.Key
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.extensions.invokeLater
import org.jetbrains.plugins.scala.project._
import org.jetbrains.plugins.scala.worksheet.actions.WorksheetFileHook
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompiler.{EvaluationCallback, WorksheetCompilerResult}
import org.jetbrains.plugins.scala.worksheet.server.RemoteServerConnector
import org.jetbrains.plugins.scala.worksheet.ui.printers.WorksheetEditorPrinter

import scala.util.control.NonFatal

private object WorksheetCompilerLocalEvaluator {

  private val RunnerClassName = "org.jetbrains.plugins.scala.runners.worksheet.PlainWorksheetRunner"

  // this method is only used when run type is [[org.jetbrains.plugins.scala.worksheet.server.OutOfProcess]]
   def executeWorksheet(file: VirtualFile, mainClassName: String, addToCp: String)
                       (callback: EvaluationCallback, worksheetPrinter: WorksheetEditorPrinter)
                       (implicit module: Module): Unit =
    invokeLater {
      try {
        val params = createJavaParameters(file, mainClassName, addToCp)
        val processHandler = createProcessHandler(params)

        WorksheetFileHook.updateStoppableProcess(file, Some(() => processHandler.destroyProcess()))
        processHandler.addProcessListener(new ProcessListener {
          override def processTerminated(event: ProcessEvent): Unit =
            WorksheetFileHook.updateStoppableProcess(file, None)
        })

        processHandler.addProcessListener(processListener(callback, worksheetPrinter))

        processHandler.startNotify()
      } catch {
        case NonFatal(ex: Throwable) =>
          callback(WorksheetCompilerResult.UnknownError(ex))
          throw ex
      }
    }

  private def createJavaParameters(originalFile: VirtualFile, mainClassName: String, addToCp: String)
                                  (implicit module: Module): OwnJavaParameters =
    createDefaultParameters(originalFile.getCanonicalPath, mainClassName, addToCp)

  private def createDefaultParameters(originalFilePath: String, mainClassName: String, addToCp: String)
                                     (implicit module: Module): OwnJavaParameters =
    createParameters(
      module = module,
      mainClassName = mainClassName,
      workingDirectory = Option(module.getProject.getBaseDir).fold("")(_.getPath),
      additionalCp = addToCp,
      worksheetField = originalFilePath
    )

  private def createParameters(module: Module,
                               mainClassName: String,
                               workingDirectory: String,
                               additionalCp: String,
                               worksheetField: String): OwnJavaParameters = {
    // upstream: CantRunException.noJdkForModule if the module SDK is not a Java SDK
    val jdk = JavaParametersUtil.createModuleJdk(module, false, null)

    val params = new OwnJavaParameters()

    params.getClassPath.addScalaCompilerClassPath(module): @scala.annotation.nowarn("cat=deprecation")
    // upstream: params.getClassPath.addRunners() (lib/runners.jar)
    params.getClassPath.add(RemoteServerConnector.RunnersJar.toString)
    params.setWorkingDirectory(workingDirectory)
    params.setMainClass(RunnerClassName)
    params.configureByModule(module, OwnJavaParameters.JDK_AND_CLASSES_AND_TESTS, jdk)

    params.getClassPath.add(additionalCp)
    params.getProgramParametersList.addParametersString(worksheetField)
    params.getProgramParametersList.prepend(mainClassName) //IMPORTANT! this must be first program argument

    params
  }

  private def createProcessHandler(params: OwnJavaParameters): ProcessHandler = {
    val processHandler = ProcessHandlerBuilder.create(params.toCommandLine).killable().build()
    ProcessTerminatedListener.attach(processHandler)
    processHandler
  }

  /**
   * upstream: the process handler reads the output with BaseOutputReader options splitToLines = true and
   * sendIncompleteLines = false (SCL-17363: WorksheetEditorPrinterPlain.processLine relies on complete lines),
   * here the output is split to lines by the listener
   */
  private def processListener(callback: EvaluationCallback, worksheetPrinter: WorksheetEditorPrinter): ProcessListener =
    new ProcessListener {
      private val incompleteLine = new java.lang.StringBuilder

      override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit = {
        if (outputType == ProcessOutputType.STDOUT) {
          incompleteLine.synchronized {
            incompleteLine.append(event.getText)
            var lineEnd = incompleteLine.indexOf("\n")
            while (lineEnd >= 0) {
              val line = incompleteLine.substring(0, lineEnd + 1)
              incompleteLine.delete(0, lineEnd + 1)
              worksheetPrinter.processLine(line)
              lineEnd = incompleteLine.indexOf("\n")
            }
          }
        }
      }

      override def processTerminated(event: ProcessEvent): Unit = {
        incompleteLine.synchronized {
          if (incompleteLine.length > 0) {
            worksheetPrinter.processLine(incompleteLine.toString)
            incompleteLine.setLength(0)
          }
        }
        worksheetPrinter.flushBuffer()
        val result = event.getExitCode match {
          case 0  => WorksheetCompilerResult.CompiledAndEvaluated
          case rc => WorksheetCompilerResult.ProcessTerminatedError(rc, event.getText)
        }
        callback.apply(result)
      }
    }
}
