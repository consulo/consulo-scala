package org.jetbrains.plugins.scala
package testingSupport.test

import consulo.disposer.Disposable
import consulo.execution.configuration.fragment.{RunConfigurationFragment, RunConfigurationFragmentEditor, RunConfigurationFragmentHost, RunConfigurationFragmentLayout, StandardRunConfigurationFragmentOrder}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.plugins.scala.testingSupport.TestingSupportBundle
import org.jetbrains.plugins.scala.testingSupport.test.ui.TestRunConfigurationForm

/**
 * upstream: AbstractTestRunConfigurationEditor (SettingsEditor with TestRunConfigurationForm)
 */
class AbstractTestRunConfigurationFragment extends RunConfigurationFragment[AbstractTestRunConfiguration] {

  override def getId: String = "scala.test.target"

  override def getName: LocalizeValue =
    LocalizeValue.of(TestingSupportBundle.message("test.run.config.test.kind").replace("&", "").stripSuffix(":"))

  override def getOrder: Int = StandardRunConfigurationFragmentOrder.FIRST

  override def isRemovable: Boolean = false

  @RequiredUIAccess
  override def createEditor(project: Project, parent: Disposable): RunConfigurationFragmentEditor[AbstractTestRunConfiguration] =
    new RunConfigurationFragmentEditor[AbstractTestRunConfiguration] {
      private val form = new TestRunConfigurationForm(project)

      @RequiredUIAccess
      override def addRows(layout: RunConfigurationFragmentLayout): Unit = {
        layout.addLabeled(getName, form.testKindComponent)
        layout.addUnlabeled(form.sbtOptionsComponent)
        layout.addUnlabeled(form.formComponent)
        layout.addUnlabeled(form.showProgressMessagesComponent)
      }

      @RequiredUIAccess
      override def attach(host: RunConfigurationFragmentHost): Unit = form.attach(host)

      @RequiredUIAccess
      override def reset(configuration: AbstractTestRunConfiguration): Unit = form.resetFrom(configuration)

      @RequiredUIAccess
      override def apply(configuration: AbstractTestRunConfiguration): Unit = form.applyTo(configuration)
    }
}
