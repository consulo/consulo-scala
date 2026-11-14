package org.jetbrains.sbt.shell.process.utils

import consulo.execution.CantRunException
import consulo.project.ui.notification.{Notification, NotificationAction, NotificationType}
import consulo.ui.ex.action.AnActionEvent
import com.intellij.openapi.options.ex.SingleConfigurableEditor
import com.intellij.openapi.options.newEditor.SettingsDialog
import consulo.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import consulo.content.bundle.Sdk
import consulo.module.content.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import consulo.ui.ex.awt.DialogWrapper.DialogStyle
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.project.external.{JdkByName, SdkUtils}
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.shell.process.utils.SbtShellJdkSelector.ConfigureProjectJdkAction

import java.nio.file.Path

final class SbtShellJdkSelector(project: Project) {

  @throws[CantRunException]
  def selectVmExecutableForSettings(sbtSettings: SbtExecutionSettings): Path = {
    val configuredOrProjectJdk = getJdkConfiguredByNameOrProjectJdk(sbtSettings)
    val customVmPath = sbtSettings.getCustomVMExecutableOrWarn(project)
    customVmPath.getOrElse {
      getJdkVmExecutablePathPath(configuredOrProjectJdk)
    }
  }

  @throws[CantRunException]
  private def getJdkVmExecutablePathPath(sdk: Sdk): Path = {
    sdk.getSdkType match {
      case sdkType: JavaSdkType =>
        Path.of(sdkType.getVMExecutablePath(sdk))
      case _ =>
        // The exception looks scary, but in the previous implementation, it was also thrown if the JDK was not of the JavaSdkType
        // (see com.intellij.openapi.projectRoots.JdkCommandLineSetup.setupJavaExePath)
        throw CantRunException.jdkMisconfigured(sdk)
    }
  }

  private def getJdkConfiguredByNameOrProjectJdk(settings: SbtExecutionSettings): Sdk = {
    val configuredJdk = findJdkByNameStoredInSettings(settings)
    val configuredOrProjectJdk = configuredJdk.orElse(getProjectJdk)
    configuredOrProjectJdk.getOrElse {
      val message = SbtBundle.message("sbt.shell.no.project.jdk.configured")
      showNoProjectJdkNotification(message)
      // Q: can we also use `CantRunException` here?
      throw new RuntimeException(message)
    }
  }

  private def findJdkByNameStoredInSettings(settings: SbtExecutionSettings): Option[Sdk] = {
    val jdkByName = settings.jdk.map(JdkByName(_))
    jdkByName.flatMap(SdkUtils.findProjectSdk(_, project))
  }

  private def getProjectJdk: Option[Sdk] =
    Option(ProjectRootManager.getInstance(project).getProjectSdk)

  private def showNoProjectJdkNotification(@Nls message: String): Unit = {
    val notification = ScalaNotificationGroups.sbtShell.createNotification(message, NotificationType.ERROR)
    notification.addAction(new ConfigureProjectJdkAction(project))
    notification.notify(project)
  }
}

object SbtShellJdkSelector {

  private class ConfigureProjectJdkAction(
    project: Project
  ) extends NotificationAction(SbtBundle.message("sbt.shell.configure.project.jdk")) {

    /** copied from [[com.intellij.ide.actions.ShowStructureSettingsAction]] */
    override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
      new ShowProjectStructureDialog(project).show()
      notification.expire()
    }
  }

  private class ShowProjectStructureDialog(project: Project) extends SingleConfigurableEditor(
    project,
    ProjectStructureConfigurable.getInstance(project),
    SettingsDialog.DIMENSION_KEY
  ) {
    override protected def getStyle = DialogStyle.COMPACT
  }
}