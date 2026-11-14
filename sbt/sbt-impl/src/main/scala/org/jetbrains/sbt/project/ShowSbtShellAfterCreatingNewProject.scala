package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl

import consulo.application.ApplicationManager
import consulo.externalSystem.ExternalSystemManager
import consulo.externalSystem.setting.{ExternalProjectSettings, ExternalSystemSettingsListenerEx}
import consulo.project.Project
import consulo.project.startup.StartupManager
import consulo.project.ui.util.AppUIUtil
import consulo.project.ui.wm.ToolWindowManager
import consulo.ui.ex.toolWindow.ToolWindow
import jakarta.inject.Inject
import org.jetbrains.sbt.settings.SbtSettings
import org.jetbrains.sbt.shell.SbtShellToolWindowFactory

import java.util

/**
 * This is hacky workaround to show sbt-shell tool window when a new sbt project is created.<br>
 * It was copied from [[com.intellij.openapi.externalSystem.service.ui.ExternalToolWindowManager]]
 *
 * Note that when a new project is being created these methods return `false` (UPD: or rather "sometimes can return", not 100% sure):
 *  - [[org.jetbrains.sbt.shell.SbtShellToolWindowFactory.shouldBeAvailable]]
 *  - [[com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory#shouldBeAvailable]]
 *
 * External tool window is explicitly activated in ExternalToolWindowManager.
 * However "sbt-shell" tool window is not an "external tool window" because it doesn't extend
 * [[com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory]]
 * This is why we need this custom class.
 *
 * related: SCL-19909, IDEA-287117, SCL-20978
 */
@ExtensionImpl
final class ShowSbtShellAfterCreatingNewProject @Inject()(project: Project) extends ExternalSystemSettingsListenerEx {

  override def onProjectsLinked(
    externalSystemManager: ExternalSystemManager[?, ?, ?, ?, ?],
    collection: util.Collection[? <: ExternalProjectSettings]
  ): Unit = {
    // This method is also called on the "default" project instance, before the new project is created.
    // `StartupManager#runAfterOpened` throws an exception if called on the default project instance.
    if (ApplicationManager.getApplication.isUnitTestMode || project.isDefault)
      return

    val sbtManager = externalSystemManager match {
      case manager: SbtExternalSystemManager => manager
      case _ =>
        return
    }

    StartupManager.getInstance(project).runAfterOpened(() => {
      val settings: SbtSettings = sbtManager.getSettingsProvider.apply(project)

      val sbtShellToolWindow = getSbtShellToolWindowInstance(project)
      if (sbtShellToolWindow != null) {
        setAvailable(sbtShellToolWindow)
      }
      else {
        //in some rare cases, toolwindow can be non-initialized by this moment ¯\_(ツ)_/¯
        //this hack comes from ExternalToolWindowManager
        // upstream: AppUIExecutor.onUiThread(...).expireWith(settings).later + ToolWindowManager.invokeLater
        AppUIUtil.invokeLaterIfProjectAlive(settings.getProject, () => {
          val toolWindow1 = getSbtShellToolWindowInstance(project)
          if (toolWindow1 != null) {
            setAvailable(toolWindow1)
          }
        })
      }
    })
  }

  private def setAvailable(toolWindow: ToolWindow): Unit = {
    if (!toolWindow.isAvailable) {
      AppUIUtil.invokeLaterIfProjectAlive(project, () => {
        toolWindow.setAvailable(true)
      })
    }
  }

  private def getSbtShellToolWindowInstance(project: Project): ToolWindow =
    ToolWindowManager.getInstance(project).getToolWindow(SbtShellToolWindowFactory.ID)

  override def onProjectsUnlinked(
    externalSystemManager: ExternalSystemManager[?, ?, ?, ?, ?],
    set: util.Set[String]
  ): Unit = {
    val sbtManager = externalSystemManager match {
      case manager: SbtExternalSystemManager => manager
      case _ =>
        return
    }
    val settings: SbtSettings = sbtManager.getSettingsProvider.apply(project)

    if (!settings.getLinkedProjectsSettings.isEmpty)
      return

    val toolWindow = getSbtShellToolWindowInstance(project)
    if (toolWindow != null) {
      // upstream: AppUIExecutor.onUiThread.expireWith(settings).expireWith(toolWindow.getDisposable)
      AppUIUtil.invokeLaterIfProjectAlive(project, () => toolWindow.setAvailable(false))
    }
  }
}
