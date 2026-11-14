package org.jetbrains.plugins.scala.server.compiler
import consulo.application.ApplicationManager
import consulo.logging.Logger

private object CompileServerShutdown {

  private[compiler] final val Log: Logger = Logger.getInstance(classOf[CompileServerShutdown.type])

  def registerShutdownTask(): Unit =
    ApplicationManager.getApplication.getInstance(classOf[CompileServerShutdownRegisterShutdownTaskService])
}
