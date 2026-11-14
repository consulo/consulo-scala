package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.{Inject, Singleton}
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{Notification, NotificationAction, NotificationService, NotificationType}
import consulo.ui.ex.action.AnActionEvent
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.component.util.CompositeModificationTracker
import org.jetbrains.plugins.scala.caches.cached
import org.jetbrains.plugins.scala.extensions.executeOnPooledThread
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings

// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class CompileServerNotificationsService @Inject()(project: Project) {

  private val title = ServerManagementBundle.message("scala.compile.server.title")
  
  private val modificationTracker = new CompositeModificationTracker(ProjectRootManager.getInstance(project))
  
  def resetNotifications(): Unit =
    modificationTracker.incModificationCount()

  /**
   * SCL-18187
   * SCL-17817
   */
  def warnIfCompileServerJdkMayLeadToCompilationProblems(): Unit = _warnIfCompileServerJdkMayLeadToCompilationProblems()

  private val _warnIfCompileServerJdkMayLeadToCompilationProblems = cached("warnIfCompileServerJdkMayLeadToCompilationProblems", modificationTracker, () => {
    if (project.hasScala) {
      for {
        (serverSdk, serverJdkVersion) <- CompileServerJdkManager.compileServerJdk(project)
        (recommendedSdk, _) = CompileServerJdkManager.recommendedJdk(project)
        if !CompileServerJdkManager.isCompatible(serverJdkVersion) || !CompileServerJdkManager.isRecommendedVersionForProject(project, serverJdkVersion)
      } showWarning(serverSdk.getName, recommendedSdk.getName)
    }
  })

  private def showWarning(serverSdk: String, recommendedSdk: String): Unit = {
    val text = s"""We recommend using JDK '$recommendedSdk' to prevent compilation issues (current JDK is '$serverSdk')""".stripMargin
    NotificationService.getInstance
      .newOfType(CompileServerNotificationGroups.compileServer, NotificationType.WARNING)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(text))
      .addAction(new FixSdkAction(recommendedSdk))
      .addAction(new OpenScalaCompileServerSettingsAction(project, filter = "JDK"))
      .notify(project)
  }
  
  private class FixSdkAction(fixedSdk: String) extends NotificationAction(LocalizeValue.of(ServerManagementBundle.message("wrong.jdk.action.use.jdk", fixedSdk))) {

    override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
      notification.expire()
      val settings = ScalaCompileServerSettings.getInstance
      if (CompileServerLauncher.defaultSdk(project).getName == fixedSdk) {
        settings.USE_DEFAULT_SDK = true
      } else {
        settings.USE_DEFAULT_SDK = false
        settings.COMPILE_SERVER_SDK = fixedSdk
      }
      ApplicationManager.getApplication.saveSettings()
      executeOnPooledThread {
        CompileServerLauncher.stopServerAndWait()
        CompileServerLauncher.ensureServerRunning(project)
      }
    }
  }
}

object CompileServerNotificationsService {
  
  def get(project: Project): CompileServerNotificationsService =
    project.getInstance(classOf[CompileServerNotificationsService])
}
