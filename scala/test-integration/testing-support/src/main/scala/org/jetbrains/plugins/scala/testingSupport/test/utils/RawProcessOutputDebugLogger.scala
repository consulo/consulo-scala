package org.jetbrains.plugins.scala.testingSupport.test.utils

import consulo.process.event.{ProcessEvent, ProcessListener}
import consulo.process.ProcessHandler
import consulo.logging.Logger
import consulo.util.dataholder.Key
import consulo.application.util.registry.Registry
import org.jetbrains.plugins.scala.testingSupport.test.utils.RawProcessOutputDebugLogger.Log

final class RawProcessOutputDebugLogger extends ProcessListener {

  override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit = {
    val eventText = event.getText
    if (eventText.trim.nonEmpty) {
      val message = s"[$outputType] $eventText"
      Log.info(message)
    }
  }
}

object RawProcessOutputDebugLogger {

  private val Log = Logger.getInstance(classOf[RawProcessOutputDebugLogger])

  // set to true to debug raw process output, can be useful to test teamcity service messages
  private def enabled: Boolean =
    Registry.get("scala.test.framework.runner.log.raw.process.output").asBoolean()

  def maybeAddListenerTo(processHandler: ProcessHandler): Unit = {
    if (enabled) {
      addListenerTo(processHandler)
    }
  }

  def addListenerTo(processHandler: ProcessHandler): Unit =
    processHandler.addProcessListener(new RawProcessOutputDebugLogger)
}
