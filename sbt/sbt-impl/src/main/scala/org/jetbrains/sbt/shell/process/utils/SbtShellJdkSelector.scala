package org.jetbrains.sbt.shell.process.utils

import consulo.execution.CantRunException
import consulo.project.ui.notification.{Notification, NotificationAction, NotificationType}
import consulo.ui.ex.action.AnActionEvent
import consulo.application.util.SystemInfo
import consulo.ide.setting.ShowSettingsUtil
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.localize.LocalizeValue
import consulo.module.ModuleManager
import consulo.project.Project
import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.content.bundle.Sdk
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
        // upstream: JavaSdkType.getVMExecutablePath
        Path.of(sdkType.getBinPath(sdk)).resolve(if (SystemInfo.isWindows) "java.exe" else "java")
      case _ =>
        // The exception looks scary, but in the previous implementation, it was also thrown if the JDK was not of the JavaSdkType
        // (see com.intellij.openapi.projectRoots.JdkCommandLineSetup.setupJavaExePath)
        throw CantRunException.jdkMisconfigured(sdk, null)
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

  // upstream: the project JDK (ProjectRootManager.getProjectSdk), Consulo modules have own JDKs - the JDK of the first module having one
  private def getProjectJdk: Option[Sdk] =
    ModuleManager.getInstance(project).getModules.iterator
      .flatMap(module => Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]])))
      .nextOption()

  private def showNoProjectJdkNotification(@Nls message: String): Unit = {
    val notification = ScalaNotificationGroups.sbtShell.createNotification(message, NotificationType.ERROR)
    notification.addAction(new ConfigureProjectJdkAction(project))
    notification.notify(project)
  }
}

object SbtShellJdkSelector {

  private class ConfigureProjectJdkAction(
    project: Project
  ) extends NotificationAction(LocalizeValue.of(SbtBundle.message("sbt.shell.configure.project.jdk"))) {

    override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
      ShowSettingsUtil.getInstance.showProjectStructureDialog(project)
      notification.expire()
    }
  }
}