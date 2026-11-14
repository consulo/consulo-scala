package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.project.ui.notification.NotificationType
import consulo.disposer.Disposable
import consulo.application.ApplicationManager
import consulo.ui.ex.awt.util.{MergingUpdateQueue, Update}

import java.util.concurrent.locks.{Lock, ReentrantLock}

// upstream: @Service(Array(Service.Level.APP))
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
private final class CompileServerErrorNotificationService extends Disposable {

  private val errorNotificationUpdateQueue: MergingUpdateQueue =
    new MergingUpdateQueue("ErrorNotificationQueue", 1000, true, MergingUpdateQueue.ANY_COMPONENT, this)

  private val errorsBuffer: java.lang.StringBuilder = new java.lang.StringBuilder()

  private val errorsBufferLock: Lock = new ReentrantLock()

  private val showNotificationUpdate: Update = new Update(this) {
    override def run(): Unit = {
      errorsBufferLock.lock()
      val text = try {
        val t = errorsBuffer.toString
        errorsBuffer.setLength(0)
        t
      } finally {
        errorsBufferLock.unlock()
      }

      // upstream: @NlsSafe
      val message = text.replace(System.lineSeparator(), "<br/>")
      CompileServerNotifications.showNotification(message, NotificationType.ERROR, project = None)
    }
  }

  override def dispose(): Unit = {}

  def onError(errorsText: String): Unit = {
    errorsBufferLock.lock()
    try errorsBuffer.append(errorsText)
    finally errorsBufferLock.unlock()
    errorNotificationUpdateQueue.queue(showNotificationUpdate)
  }
}

private object CompileServerErrorNotificationService {
  def instance(): CompileServerErrorNotificationService =
    ApplicationManager.getApplication.getInstance(classOf[CompileServerErrorNotificationService])
}
