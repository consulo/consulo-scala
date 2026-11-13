package org.jetbrains.plugins.scala.lang.scaladoc.generate

import consulo.ui.image.Image
import consulo.language.editor.scope.AnalysisScope
import consulo.execution.executor.Executor
import consulo.execution.configuration.{ModuleRunProfile, RunProfileState}
import consulo.execution.runner.ExecutionEnvironment
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle


class ScaladocConfiguration(private val form: ScaladocConsoleRunConfigurationForm, private val project: Project,
                            private val scope: AnalysisScope) extends ModuleRunProfile {

  override def getName: String = ScalaBundle.message("generate.scaladoc")

  override def getIcon: Image = null

  override def getModules: Array[Module] = Module.EMPTY_ARRAY

  override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState = {
    val state: ScaladocCommandLineState = new ScaladocCommandLineState(env, project)
    state.setAdditionalScaladocFlags(form.getAdditionalFlags)
    state.setScope(scope)
    state.setVerbose(form.isVerbose)
    state.setDocTitle(form.getDocTitle)
    state.setMaxHeapSize(form.getMaxHeapSize)
    state.setShowInBrowser(form.isShowInBrowser)
    state.setOutputDir(form.getOutputDir)
    state
  }
}