package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.disposer.Disposable
import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.platform.eel.provider.utils.EelProjectUtils
import org.jetbrains.plugins.scala.util.ScalaShutDownTracker

import scala.concurrent.duration.Duration

private object CompileServerShutdown {

  private final val Log: Logger = Logger.getInstance(classOf[CompileServerShutdown.type])

  // upstream: @Service(Array(Service.Level.APP))
  @Singleton
  @ServiceAPI(ComponentScope.APPLICATION)
  @ServiceImpl
  private final class RegisterShutdownTaskService extends Disposable {
    // Guaranteed to run at most-once, when the service is initialized.
    ScalaShutDownTracker.registerShutdownTask(() => {
      Log.info("Shutdown event triggered, stopping server")
      CompileServerLauncher.stopServerAndWaitFor(Duration.Zero)
    })

    ApplicationManager.getApplication.getMessageBus.connect(this).subscribe(ProjectCloseListener.TOPIC, new ProjectCloseListener {
      override def projectClosing(project: Project): Unit = {
        //noinspection ApiStatus,UnstableApiUsage
        val local = EelProjectUtils.isProjectLocal(project)
        if (!local) {
          // Only stop the server if the remote eel project is closing. We don't want to leak the process running
          // in a remote eel with no option to stop it after the project is closed.
          CompileServerLauncher.stopServerAndWaitFor(Duration.Zero)
        }
      }
    })

    override def dispose(): Unit = {}
  }

  def registerShutdownTask(): Unit =
    ApplicationManager.getApplication.getInstance(classOf[RegisterShutdownTaskService])
}
