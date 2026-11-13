package org.jetbrains.plugins.scala.lang.scaladoc.generate

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef

import consulo.localize.LocalizeValue
import consulo.application.CommonBundle
import consulo.language.editor.scope.AnalysisScope
import consulo.language.editor.impl.action.BaseAnalysisAction
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog
import consulo.execution.executor.DefaultRunExecutor
import consulo.execution.runner.ExecutionEnvironmentBuilder
import consulo.execution.ui.awt.ExecutionErrorDialog
import consulo.process.ExecutionException
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.event.DocumentAdapter
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.VerticalLayout
import org.jetbrains.plugins.scala.ScalaBundle

import javax.swing.event.DocumentEvent

@ActionImpl(id = "Scala.GenerateScaladoc", parents = Array(new ActionParentRef(value = new ActionRef(id = "ToolsMenu"))))
class ScaladocAction extends BaseAnalysisAction(
  LocalizeValue.of(ScalaBundle.message("generate.scaladoc.action.text")),
  LocalizeValue.of(ScalaBundle.message("generate.scaladoc.action.description")),
  LocalizeValue.of(ScalaBundle.message("generate.scaladoc")),
  LocalizeValue.of(ScalaBundle.message("scaladoc.noon"))
) {
  private var configurationDialog: ScaladocConsoleRunConfigurationForm = scala.compiletime.uninitialized

  private def disposeForm(): Unit = {
    configurationDialog = null
  }

  override def analyze(project: Project, scope: AnalysisScope): Unit = {
    try {
      configurationDialog.saveSettings()
      val config = new ScaladocConfiguration(configurationDialog, project, scope)
      // upstream: runs the profile via RunnerAndConfigurationSettingsImpl/RunManagerImpl (internal in Consulo)
      try {
        ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor.getRunExecutorInstance, config).buildAndExecute()
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

  // upstream: getAdditionalActionSettings returning a Swing component
  @RequiredUIAccess
  override protected def extendMainLayout(dialog: BaseAnalysisActionDialog, layout: VerticalLayout, project: Project): Unit = {
    configurationDialog = new ScaladocConsoleRunConfigurationForm(project)
    configurationDialog.getOutputDirChooser.getDocument.addDocumentListener(new DocumentAdapter() {
      override def textChanged(e: DocumentEvent): Unit = {
        updateAvailability(dialog)
      }
    })
    updateAvailability(dialog)
    layout.add(TargetAWT.wrap(configurationDialog.createCenterPanel()))
  }

  private def updateAvailability(dialog: BaseAnalysisActionDialog): Unit = {
    dialog.setOKActionEnabled(!configurationDialog.getOutputDir.isEmpty)
  }
}
