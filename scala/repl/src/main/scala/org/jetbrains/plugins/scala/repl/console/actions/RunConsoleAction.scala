package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import com.intellij.execution.{ExecutionBundle, RunManager, RunManagerEx, RunnerAndConfigurationSettings}
import consulo.process.ExecutionException
import com.intellij.execution.configurations.{ConfigurationType, ConfigurationTypeUtil, RunConfiguration}
import consulo.execution.executor.DefaultRunExecutor
import consulo.execution.runner.{ExecutionEnvironmentBuilder, ProgramRunner}
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
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
) with RunConsoleAction.RunActionBase[ScalaConsoleConfigurationType] {

  override protected def getNewSettingName: String = ScalaReplBundle.message("scala.console.actions.scala.repl")

  override def update(e: AnActionEvent): Unit = {
    if (e.getProject == null || e.getProject.isDisposed)
      return

    val isEnabledAndVisible = e.getProject.hasScala
    e.getPresentation.setEnabledAndVisible(isEnabledAndVisible)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit =
    doRunAction(e)

  override protected def getMyConfigurationType: ScalaConsoleConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[ScalaConsoleConfigurationType])
}

object RunConsoleAction {

  private def runFromSetting(setting: RunnerAndConfigurationSettings, runManagerEx: RunManagerEx, project: Project): Unit = {
    val configuration = setting.getConfiguration
    runManagerEx.setTemporaryConfiguration(setting)
    val runExecutor = DefaultRunExecutor.getRunExecutorInstance
    val runner = ProgramRunner.getRunner(runExecutor.getId, configuration)
    if (runner != null) {
      try {
        val builder: ExecutionEnvironmentBuilder = new ExecutionEnvironmentBuilder(project, runExecutor)
        builder.runnerAndSettings(runner, setting)
        runner.execute(builder.build())
      }
      catch {
        case e: ExecutionException =>
          //noinspection ReferencePassedToNls
          Messages.showErrorDialog(project, e.getMessage, ExecutionBundle.message("error.common.title"))
      }
    }
  }

  def runExisting(setting: RunnerAndConfigurationSettings, runManagerEx: RunManagerEx, project: Project): Unit =
    inReadAction {
      runFromSetting(setting, runManagerEx, project)
    }

  def createAndRun(configurationType: ConfigurationType, runManagerEx: RunManagerEx,
                   project: Project, name: String, handler: RunConfiguration => Unit): Unit =
    inReadAction {
      val factory  = configurationType.getConfigurationFactories.apply(0)
      val setting = RunManager.getInstance(project).createConfiguration(name, factory)
      handler(setting.getConfiguration)
      runFromSetting(setting, runManagerEx, project)
    }

  trait RunActionBase[T <: ConfigurationType] {
    protected def getMyConfigurationType: T

    protected def getNewSettingName: String

    protected def getRunConfigurationHandler: RunConfiguration => Unit = (_: RunConfiguration) => {}

    protected def checkFile(@Nullable psiFile: PsiFile): Boolean = true

    protected final def doRunAction(e: AnActionEvent): Unit = {
      val dataContext = e.getDataContext
      val file = CommonDataKeys.PSI_FILE.getData(dataContext)
      val project = CommonDataKeys.PROJECT.getData(dataContext)

      if (project == null || !checkFile(file)) return

      val runManagerEx = RunManagerEx.getInstanceEx(project)
      val configurationType = getMyConfigurationType
      val settings = runManagerEx.getConfigurationSettingsList(configurationType).asScala

      settings.headOption match {
        case Some(setting) =>
          RunConsoleAction.runExisting(setting, runManagerEx, project)
        case _ =>
          RunConsoleAction.createAndRun(configurationType, runManagerEx, project, getNewSettingName, getRunConfigurationHandler)
      }
    }
  }
}
