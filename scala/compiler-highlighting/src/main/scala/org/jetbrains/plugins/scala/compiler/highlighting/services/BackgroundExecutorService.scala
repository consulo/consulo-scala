package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.disposer.Disposable
import consulo.project.Project
import jakarta.inject.Singleton

import java.util.concurrent.ExecutorService

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class BackgroundExecutorService extends Disposable {

  val executor: ExecutorService = {
    val backendExecutor = AppExecutorUtil.getAppExecutorService
    AppExecutorUtil.createBoundedApplicationPoolExecutor("Scala Compiler Based Highlighting background executor", backendExecutor, 1, this)
  }

  private def executeOnBackgroundThread(runnable: Runnable): Unit = {
    executor.execute(runnable)
  }

  override def dispose(): Unit = {}
}

private[highlighting] object BackgroundExecutorService {
  def executeOnBackgroundThreadInNotDisposed(project: Project)(action: => Unit): Unit = {
    if (project.isDisposed)
      return

    instance(project).executeOnBackgroundThread(() => {
      if (!project.isDisposed) {
        action
      }
    })
  }

  def instance(project: Project): BackgroundExecutorService =
    project.getInstance(classOf[BackgroundExecutorService])
}
