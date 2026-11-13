package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.localize.LocalizeValue
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel
import consulo.project.ui.notification.{Notification, NotificationAction, NotificationGroup, NotificationType}
import consulo.ui.ex.action.AnActionEvent
import consulo.component.persist.{PersistentStateComponent, Storage, StoragePathMacros}
import com.intellij.openapi.components.Service
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.formatting.scalafmt
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups

import scala.beans.BeanProperty

@State(
  name = "SuggestScalaFmt",
  storages = Array[Storage](new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@Service(Array(Service.Level.PROJECT))
final class ScalaFmtSuggesterService(private val project: Project)
  extends PersistentStateComponent[ScalaFmtSuggesterService.State] {

  import ScalaFmtSuggesterService._

  def init(): Unit =
    if (needToSuggestScalafmtFormatter(project)) {
      createNotification.notify(project)
    }

  private def needToSuggestScalafmtFormatter(project: Project): Boolean = {
    val settings = ScalaCodeStyleSettings.getInstance(project)
    !settings.USE_SCALAFMT_FORMATTER &&
      projectHasScalafmtDefaultConfigFile &&
      state.enableForCurrentProject
  }

  private def projectHasScalafmtDefaultConfigFile: Boolean = {
    project.baseDir.toOption
      .flatMap(_.findChild(ScalafmtDynamicConfigService.DefaultConfigurationFileName).toOption)
      .nonEmpty
  }

  private var state: ScalaFmtSuggesterService.State = new ScalaFmtSuggesterService.State()
  override def getState: ScalaFmtSuggesterService.State = state
  override def loadState(state: ScalaFmtSuggesterService.State): Unit = this.state = state

  private def enableForProject(): Unit = {
    val codeStyleSchemesModel = new CodeStyleSchemesModel(project)
    var scheme = codeStyleSchemesModel.getSelectedScheme
    if (!codeStyleSchemesModel.isProjectScheme(scheme)) {
      codeStyleSchemesModel.copyToProject(scheme)
      scheme = codeStyleSchemesModel.getProjectScheme
    }
    val newSettings = scheme.getCodeStyleSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    newSettings.FORMATTER = ScalaCodeStyleSettings.SCALAFMT_FORMATTER
    newSettings.SCALAFMT_CONFIG_PATH = ""
    codeStyleSchemesModel.apply()

    ScalafmtDynamicConfigService.instanceIn(project).init()
  }

  private def dontShow(): Unit = {
    val newState = new scalafmt.ScalaFmtSuggesterService.State()
    newState.enableForCurrentProject = false
    loadState(newState)
  }

  private def createNotification: Notification = {
    val notification = suggesterNotificationGroup.createNotification(ScalaBundle.message("scalafmt.suggester.detected.in.project"), NotificationType.INFORMATION)
    notification.addAction(new NotificationAction(LocalizeValue.of(ScalaBundle.message("scalafmt.suggester.enable"))) {
      override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
        notification.expire()
        enableForProject()
      }
    })
    notification.addAction(new NotificationAction(LocalizeValue.of(ScalaBundle.message("scalafmt.suggester.dont.show"))) {
      override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
        notification.expire()
        dontShow()
      }
    })
    notification
  }
}

object ScalaFmtSuggesterService {

  def instance(implicit project: Project): ScalaFmtSuggesterService =
    project.getInstance(classOf[ScalaFmtSuggesterService])

  class State {
    @BeanProperty
    var enableForCurrentProject: Boolean = true
  }

  private def suggesterNotificationGroup: NotificationGroup = ScalaNotificationGroups.scalaFeaturesAdvertiser
}
