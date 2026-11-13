package org.jetbrains.plugins.scala.components

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.application.ApplicationManager
import consulo.project.Project
import org.jetbrains.plugins.scala.extensions.invokeLater
import org.jetbrains.plugins.scala.startup.ProjectActivity

private final class ScalaPluginVersionVerifierProjectActivity extends ProjectActivity {
  override def execute(project: Project): Unit = {
    // Application-level services are initialised exactly once.
    ApplicationManager.getApplication.getInstance(classOf[ScalaPluginVersionVerifierProjectActivity.AppService])
  }
}

private object ScalaPluginVersionVerifierProjectActivity {
  // upstream: @Service(Array(Service.Level.APP))
  @Singleton
  @ServiceAPI(ComponentScope.APPLICATION)
  @ServiceImpl
  private final class AppService {
    // Executed in the service constructor.
    initPluginUpdater()

    private def initPluginUpdater(): Unit = invokeLater {
      ScalaPluginUpdater.askUpdatePluginBranchIfNeeded()
      ScalaPluginUpdater.postCheckIdeaCompatibility()
    }
  }
}
