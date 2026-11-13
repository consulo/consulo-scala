package org.jetbrains.plugins.scala.project.external

import consulo.project.ui.notification.{NotificationGroup, NotificationService}
import consulo.application.ApplicationManager
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, Key, ProjectKeys, ProjectSystemId}
import consulo.externalSystem.service.notification.{NotificationCategory, NotificationData, NotificationSource}
import consulo.externalSystem.service.project.manage.ProjectDataService
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.localize.LocalizeValue
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.ScalaBundle

// upstream: extends AbstractProjectDataService (with IdeModifiableModelsProvider)
abstract class ScalaAbstractProjectDataService[E, I](key: Key[E]) extends ProjectDataService[E, I] {

  override def getTargetDataKey: Key[E] = key

  // upstream: implicit class over IdeModifiableModelsProvider, Consulo data services find modules in the ModuleManager
  protected implicit class IdeModifiableModelsProviderOps(private val project: Project) {

    def findIdeModuleOpt(name: String): Option[Module] =
      Option(ModuleManager.getInstance(project).findModuleByName(name))

    def findIdeModuleOpt(data: ModuleData): Option[Module] =
      findIdeModuleOpt(data.getInternalName)

    def getIdeModuleByNode(node: DataNode[?]): Option[Module] = {
      val moduleNode = Iterator.iterate[DataNode[?]](node.getParent)(_.getParent)
        .takeWhile(_ != null)
        .find(_.getData.isInstanceOf[ModuleData])
      val key = moduleNode
        .map(_.getKey.asInstanceOf[Key[? <: ModuleData]])
        .getOrElse(ProjectKeys.MODULE)
      for {
        moduleData <- Option[ModuleData](node.getData(key))
        module <- findIdeModuleOpt(moduleData)
      } yield module
    }
  }

  protected final def executeProjectChangeAction(project: Project)(action: => Unit): Unit =
    ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
      override def execute(): Unit = action
    })

  protected final def executeProjectChangeAction(action: => Unit)(implicit project: ProjectContext): Unit =
    executeProjectChangeAction(project.project)(action)

  protected def showScalaLibraryNotFoundWarning(
    title: NlsString,
    version: String,
    module: String,
    balloonGroup: NotificationGroup,
    systemId: ProjectSystemId,
  )(implicit project: Project): Unit = {
    showWarning(
      title,
      NlsString(ScalaBundle.message("scala.project.data.service.scalaLibraryNotFound", module, version)),
      balloonGroup,
      systemId
    )
  }

  protected final def showWarning(
    title: NlsString,
    message: NlsString,
    balloonGroup: NotificationGroup,
    systemId: ProjectSystemId
  )(implicit project: Project): Unit = {
    val notificationData = new NotificationData(
      title.nls,
      message.nls,
      NotificationCategory.WARNING,
      NotificationSource.PROJECT_SYNC
    )
    // upstream: notificationData.setBalloonGroup(balloonGroup) + ExternalSystemNotificationManager.showNotification(systemId, notificationData)
    NotificationService.getInstance
      .newWarn(balloonGroup)
      .title(LocalizeValue.of(title.nls))
      .content(LocalizeValue.of(message.nls))
      .notify(project)

    if (ApplicationManager.getApplication.isUnitTestMode) {
      val notificationBefore = Option(project.getUserData(ShownNotificationsKey)).getOrElse(Nil)
      val notificationsNew = notificationBefore :+ ShownNotification(systemId, notificationData)
      project.putUserData(ShownNotificationsKey, notificationsNew)
    }
  }
}
