package org.jetbrains.sbt.shell.action

import consulo.annotation.component.ActionImpl

import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent, AnActionWithSyncUpdate, DumbAwareAction}
import consulo.externalSystem.model.{ExternalSystemDataKeys, ProjectSystemId}
import consulo.externalSystem.view.ModuleNode
import consulo.externalSystem.view.ProjectNode
import consulo.application.progress.{ProgressIndicator, ProgressManager, Task}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.annotations.NonNls
import org.jetbrains.sbt.project.data.{SbtCommandData, SbtNamedKey, SbtSettingData, SbtTaskData}
import org.jetbrains.sbt.shell.{CancelableWaitUtil, SbtShellCommunication}
import org.jetbrains.sbt.shell.action.SbtNodeAction.*
import org.jetbrains.sbt.shell.communication.SbtShellCommandRequest
import org.jetbrains.sbt.{SbtBundle, SbtUtil}

import scala.concurrent.Promise
import scala.jdk.CollectionConverters.*

// upstream: extends ExternalSystemNodeAction (not in Consulo)
abstract class SbtNodeAction[T <: SbtNamedKey](c: Class[T], text: String, description: String)
  extends DumbAwareAction(LocalizeValue.of(text), LocalizeValue.of(description)) with AnActionWithSyncUpdate {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  @NonNls protected def buildCmd(@NonNls projectId: String, @NonNls key: String): String

  @RequiredUIAccess
  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabled(
      e.getData(Project.KEY) != null && e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID) != null && getExternalData(e).isDefined
    )

  @RequiredUIAccess
  override def actionPerformed(e: AnActionEvent): Unit =
    for {
      project <- Option(e.getData(Project.KEY))
      projectSystemId <- Option(e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID))
      data <- getExternalData(e)
    } perform(project, projectSystemId, data, e)

  private def getExternalData(e: AnActionEvent): Option[T] =
    Option(e.getData(ExternalSystemDataKeys.SELECTED_NODES))
      .flatMap(_.asScala.headOption)
      .map(_.getData)
      .collect { case data if c.isInstance(data) => c.cast(data) }

  protected def perform(project: Project, projectSystemId: ProjectSystemId, externalData: T, e: AnActionEvent): Unit = {
    //noinspection ScalaUnusedSymbol (unused `n` is necessary for compile to succeed)
    val projectScope = for {
      selected <- Option(e.getData(ExternalSystemDataKeys.SELECTED_NODES)).flatMap(_.asScala.headOption)
      groupNode <- Option(selected.getParent)
      case moduleNode@(_n: ModuleNode) <- Option(groupNode.getParent)
      parentProjectNode <- Option(moduleNode.findParent(classOf[ProjectNode]))
      rootProjectPath <- Option(parentProjectNode.getData).map(_.getLinkedExternalProjectPath)
      esModuleData <- Option(moduleNode.getData)
      sbtModuleData <- SbtUtil.getSbtModuleData(project, esModuleData.getId, rootProjectPath)
    } yield {
      SbtUtil.makeSbtProjectId(sbtModuleData)
    }

    val comms = SbtShellCommunication.forProject(project)
    val projectPart = projectScope.getOrElse("")
    val keyPart = externalData.name
    val cmd = buildCmd(projectPart, keyPart)

    // Running a command in the sbt shell requires a BGT because some underlying eel methods used when the shell is created need it
    val request = SbtShellCommandRequest.collectOutput(cmd)
    val task = new Task.Backgroundable(project, LocalizeValue.of(SbtBundle.message("sbt.shell.action.run.task.indicator", keyPart)), true) {
      override def run(indicator: ProgressIndicator): Unit = {
        val future = comms.runAndCollectOutput(request)
        // upstream: ProgressIndicatorUtils.awaitWithCheckCanceled, the command is removed in #onCancel
        CancelableWaitUtil.waitForCancelable(future, onCancel = () => comms.removeCommandFromQueueOrCancel(request.requestId))(Promise(), indicator)
      }
    }
    ProgressManager.getInstance().run(task)
  }
}

object SbtNodeAction {
  @NonNls def scopedKey(@NonNls project:String, @NonNls key: String): String = if (project.nonEmpty) s"$project/$key" else key
}

abstract class SbtTaskAction(text: String, description: String) extends SbtNodeAction[SbtTaskData](classOf[SbtTaskData], text, description)
abstract class SbtSettingAction(text: String, description: String) extends SbtNodeAction[SbtSettingData](classOf[SbtSettingData], text, description)
abstract class SbtCommandAction(text: String, description: String) extends SbtNodeAction[SbtCommandData](classOf[SbtCommandData], text, description)

@ActionImpl(id = "Scala.Sbt.RunTask")
class RunTaskAction extends SbtTaskAction(
  SbtBundle.message("sbt.shell.action.run.task"),
  SbtBundle.message("sbt.shell.action.run.task.description")
) {
  override def buildCmd(project: String, key: String): String = scopedKey(project,key)
}

@ActionImpl(id = "Scala.Sbt.ShowTask")
class ShowTaskAction extends SbtTaskAction(
  SbtBundle.message("sbt.shell.action.show.task"),
  SbtBundle.message("sbt.shell.action.show.task.description")
) {
  override def buildCmd(project: String, key: String): String = s"show ${scopedKey(project,key)}"
}

@ActionImpl(id = "Scala.Sbt.InspectTask")
class InspectTaskAction extends SbtTaskAction(
  SbtBundle.message("sbt.shell.action.inspect.task"),
  SbtBundle.message("sbt.shell.action.inspect.task.description")
) {
  override def buildCmd(project: String, key: String): String = s"inspect ${scopedKey(project,key)}"
}

@ActionImpl(id = "Scala.Sbt.ShowSetting")
class ShowSettingAction extends SbtSettingAction(
  SbtBundle.message("sbt.shell.action.show.setting"),
  SbtBundle.message("sbt.shell.action.show.setting.description")
) {
  override def buildCmd(project: String, key: String): String = scopedKey(project,key)
}

@ActionImpl(id = "Scala.Sbt.InspectSetting")
class InspectSettingAction extends SbtSettingAction(
  SbtBundle.message("sbt.shell.action.inspect.setting"),
  SbtBundle.message("sbt.shell.action.inspect.setting.description")
) {
  override def buildCmd(project: String, key: String): String = s"inspect ${scopedKey(project,key)}"
}

@ActionImpl(id = "Scala.Sbt.RunCommand")
class RunCommandAction extends SbtCommandAction(
  SbtBundle.message("sbt.shell.action.run.command"),
  SbtBundle.message("sbt.shell.action.run.command.description")
) {
  override def buildCmd(project: String, key: String): String = s";project $project; $key"
}

@ActionImpl(id = "Scala.Sbt.Help")
class SbtHelpAction extends SbtNodeAction[SbtNamedKey](classOf[SbtNamedKey],
  SbtBundle.message("sbt.shell.action.help"),
  SbtBundle.message("sbt.shell.action.help.description")
) {
  override def buildCmd(project: String, key: String): String = s"help $key"
}
