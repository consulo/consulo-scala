package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.disposer.Disposable
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import org.jetbrains.plugins.scala.server.compiler.CompileServerShutdown.Log
import org.jetbrains.plugins.scala.util.ScalaShutDownTracker

import scala.concurrent.duration.Duration

// upstream: @Service(Array(Service.Level.APP)) CompileServerShutdown.RegisterShutdownTaskService
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
private final class CompileServerShutdownRegisterShutdownTaskService extends Disposable {
  // Guaranteed to run at most-once, when the service is initialized.
  ScalaShutDownTracker.registerShutdownTask(() => {
    Log.info("Shutdown event triggered, stopping server")
    CompileServerLauncher.stopServerAndWaitFor(Duration.Zero)
  })

  ApplicationManager.getApplication.getMessageBus.connect(this).subscribe(classOf[ProjectManagerListener], new ProjectManagerListener {
    override def projectClosing(project: Project): Unit = {
      val local = EelCompilerUtils.isLocal(project.getPlatform)
      if (!local) {
        // Only stop the server if the remote project is closing. We don't want to leak the process running
        // on a remote machine with no option to stop it after the project is closed.
        CompileServerLauncher.stopServerAndWaitFor(Duration.Zero)
      }
    }
  })

  override def dispose(): Unit = {}
}
