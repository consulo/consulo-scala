package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.execution.{ProgramRunnerUtil, RunManager, RunnerAndConfigurationSettings}
import consulo.execution.localize.ExecutionLocalize
import consulo.process.ExecutionException
import consulo.execution.configuration.{ConfigurationType, ConfigurationTypeUtil, RunConfiguration}
import consulo.execution.executor.DefaultRunExecutor
import consulo.execution.runner.ExecutionEnvironmentBuilder
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.project.Project
import consulo.ui.ex.awt.Messages
import consulo.language.psi.PsiFile
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.repl.console.ScalaReplBundle
import org.jetbrains.plugins.scala.repl.console.configuration.ScalaConsoleConfigurationType
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project.ProjectExt

import scala.jdk.CollectionConverters.*

@ActionImpl(id = "Scala.RunConsole", parents = Array(new ActionParentRef(value = new ActionRef(id = "ToolsMenu"))))
class RunConsoleAction extends AnAction(
  ScalaReplBundle.message("scalarepl.menu.action.text"),
  ScalaReplBundle.message("scalarepl.menu.action.description"),
  Icons.SCALA_CONSOLE
) with AnActionWithSyncUpdate with RunConsoleAction.RunActionBase[ScalaConsoleConfigurationType] {

  override protected def getNewSettingName: String = ScalaReplBundle.message("scala.console.actions.scala.repl")

  override def update(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project == null || project.isDisposed)
      return

    val isEnabledAndVisible = project.hasScala
    e.getPresentation.setEnabledAndVisible(isEnabledAndVisible)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit =
    doRunAction(e)

  override protected def getMyConfigurationType: ScalaConsoleConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[ScalaConsoleConfigurationType])
}

object RunConsoleAction {

  private def runFromSetting(setting: RunnerAndConfigurationSettings, runManager: RunManager, project: Project): Unit = {
    // upstream: RunManagerEx.setTemporaryConfiguration
    setting.setTemporary(true)
    runManager.addConfiguration(setting)
    runManager.setSelectedConfiguration(setting)
    val runExecutor = DefaultRunExecutor.getRunExecutorInstance
    val runner = ProgramRunnerUtil.getRunner(runExecutor.getId, setting)
    if (runner != null) {
      try {
        val builder: ExecutionEnvironmentBuilder = new ExecutionEnvironmentBuilder(project, runExecutor)
        builder.runnerAndSettings(runner, setting)
        runner.execute(builder.build())
      }
      catch {
        case e: ExecutionException =>
          //noinspection ReferencePassedToNls
          Messages.showErrorDialog(project, e.getMessage, ExecutionLocalize.errorCommonTitle.get)
      }
    }
  }

  def runExisting(setting: RunnerAndConfigurationSettings, runManager: RunManager, project: Project): Unit =
    inReadAction {
      runFromSetting(setting, runManager, project)
    }

  def createAndRun(configurationType: ConfigurationType, runManager: RunManager,
                   project: Project, name: String, handler: RunConfiguration => Unit): Unit =
    inReadAction {
      val factory  = configurationType.getConfigurationFactories.apply(0)
      val setting = runManager.createRunConfiguration(name, factory)
      handler(setting.getConfiguration)
      runFromSetting(setting, runManager, project)
    }

  trait RunActionBase[T <: ConfigurationType] {
    protected def getMyConfigurationType: T

    protected def getNewSettingName: String

    protected def getRunConfigurationHandler: RunConfiguration => Unit = (_: RunConfiguration) => {}

    protected def checkFile(@Nullable psiFile: PsiFile): Boolean = true

    protected final def doRunAction(e: AnActionEvent): Unit = {
      val dataContext = e.getDataContext
      val file = dataContext.getData(PsiFile.KEY)
      val project = dataContext.getData(Project.KEY)

      if (project == null || !checkFile(file)) return

      val runManager = RunManager.getInstance(project)
      val configurationType = getMyConfigurationType
      val settings = runManager.getConfigurationSettingsList(configurationType).asScala

      settings.headOption match {
        case Some(setting) =>
          RunConsoleAction.runExisting(setting, runManager, project)
        case _ =>
          RunConsoleAction.createAndRun(configurationType, runManager, project, getNewSettingName, getRunConfigurationHandler)
      }
    }
  }
}
