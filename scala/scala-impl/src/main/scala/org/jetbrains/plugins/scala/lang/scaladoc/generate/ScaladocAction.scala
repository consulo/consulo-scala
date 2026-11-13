package org.jetbrains.plugins.scala.lang.scaladoc.generate

import consulo.application.CommonBundle
import consulo.language.editor.scope.AnalysisScope
import consulo.language.editor.impl.action.BaseAnalysisAction
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog
import consulo.execution.configuration.{RunConfigurationBase, RunProfileState, RunnerSettings}
import consulo.execution.executor.DefaultRunExecutor
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import consulo.execution.runner.{ExecutionEnvironment, ExecutionEnvironmentBuilder, ProgramRunner}
import consulo.execution.ui.awt.ExecutionErrorDialog
import consulo.process.ExecutionException
import consulo.execution.executor.Executor
import consulo.execution.configuration.ui.SettingsEditor
import consulo.project.Project
import consulo.ui.ex.awt.event.DocumentAdapter
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.scaladoc.generate.ScaladocAction.ScaladocRunConfiguration

import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class ScaladocAction extends BaseAnalysisAction(ScalaBundle.message("generate.scaladoc"), ScalaBundle.message("scaladoc.noon")) {
  private var configurationDialog: ScaladocConsoleRunConfigurationForm = scala.compiletime.uninitialized

  locally {
    val presentation = getTemplatePresentation
    presentation.setText(ScalaBundle.message("generate.scaladoc.action.text"))
    presentation.setDescription(ScalaBundle.message("generate.scaladoc.action.description"))
  }

  private def disposeForm(): Unit = {
    configurationDialog = null
  }

  override def analyze(project: Project, scope: AnalysisScope): Unit = {
    var config: ScaladocConfiguration = null
    try {
      configurationDialog.saveSettings()
      config = new ScaladocConfiguration(configurationDialog, project, scope)
      try {
        val runConfig = new ScaladocRunConfiguration(project, configurationDialog, config)

        val runner: ProgramRunner[? <: RunnerSettings] =
          ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, config)
        val builder: ExecutionEnvironmentBuilder =
          new ExecutionEnvironmentBuilder(project, DefaultRunExecutor.getRunExecutorInstance)
        builder.runProfile(config)
        builder.runnerAndSettings(runner,
          new RunnerAndConfigurationSettingsImpl(new RunManagerImpl(project), runConfig, false))
        runner.execute(builder.build())
      } catch {
        case e: ExecutionException => ExecutionErrorDialog.show(e, CommonBundle.getErrorTitle, project)
      }
    }
    finally {
      disposeForm()
    }
  }

  override def canceled(): Unit = {
    super.canceled()
    disposeForm()
  }

  override def getAdditionalActionSettings(project: Project, dialog: BaseAnalysisActionDialog): JComponent = {
    configurationDialog = new ScaladocConsoleRunConfigurationForm(project)
    configurationDialog.getOutputDirChooser.getDocument.addDocumentListener(new DocumentAdapter() {
      override def textChanged(e: DocumentEvent): Unit = {
        updateAvailability(dialog)
      }
    })
    updateAvailability(dialog)
    configurationDialog.createCenterPanel()
  }

  private def updateAvailability(dialog: BaseAnalysisActionDialog): Unit = {
    dialog.setOKActionEnabled(!configurationDialog.getOutputDir.isEmpty)
  }
}

object ScaladocAction {

  class ScaladocRunConfiguration(
    project: Project,
    dialog: ScaladocConsoleRunConfigurationForm,
    config: ScaladocConfiguration
  ) extends RunConfigurationBase[Unit](project, null, "Generate ScalaDoc") {
    override def checkConfiguration(): Unit = {}

    override def getConfigurationEditor: SettingsEditor[? <: ScaladocRunConfiguration] = new SettingsEditor[ScaladocRunConfiguration]() {
      override def createEditor(): JComponent = dialog.createCenterPanel()

      override def resetEditorFrom(s: ScaladocRunConfiguration): Unit = {}

      override def applyEditorTo(s: ScaladocRunConfiguration): Unit = {}
    }

    override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState = config.getState(executor, env)
  }
}