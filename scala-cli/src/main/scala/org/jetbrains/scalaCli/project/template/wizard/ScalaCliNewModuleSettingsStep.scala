package org.jetbrains.scalaCli.project.template.wizard

import consulo.application.ApplicationManager
import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.{WizardStep, WizardStepValidationException}
import consulo.ui.layout.VerticalLayout
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, Component}
import consulo.util.lang.SystemProperties
import org.jetbrains.plugins.scala.project.template.ScalaSampleCode
import org.jetbrains.sbt.project.template.wizard.ScalaVersionStepLike
import org.jetbrains.scalaCli.{ScalaCliBundle, ScalaCliUtils}

import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * Selects the Scala version of a new Scala CLI project, whether to add sample code, and checks that Scala CLI is installed.
 */
class ScalaCliNewModuleSettingsStep extends WizardStep[ScalaCliNewModuleContext] with ScalaVersionStepLike {

  private var addSampleCodeBox: CheckBox = scala.compiletime.uninitialized

  @RequiredUIAccess
  override def getComponent(context: ScalaCliNewModuleContext, uiDisposable: Disposable): Component = {
    val scalaVersionBox = createScalaVersionBox(context.scalaVersion, uiDisposable)
    addSampleCodeBox = ScalaSampleCode.createCheckBox()

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.sided(label("sbt.settings.scala"), scalaVersionBox))
    layout.add(createIndentationSyntaxCheckBox())
    layout.add(addSampleCodeBox)
    selectedScalaVersion.foreach(version => setShowIndentationSyntaxCheckBox(ScalaVersionStepLike.isScala3VersionString(version)))
    layout
  }

  @RequiredUIAccess
  override def onStepLeave(context: ScalaCliNewModuleContext): Unit = {
    selectedScalaVersion.foreach(context.scalaVersion = _)
    context.useIndentationSyntax = useIndentationSyntax
    Option(addSampleCodeBox).foreach(box => context.addSampleCode = ScalaSampleCode.isSelected(box))
  }

  @RequiredUIAccess
  override def validateStep(context: ScalaCliNewModuleContext): CompletableFuture[?] = {
    val installationPath = Path.of(SystemProperties.getUserHome)
    if (ApplicationManager.getApplication.isUnitTestMode || ScalaCliUtils.isScalaCliInstalled(installationPath))
      CompletableFuture.completedFuture(null)
    else
      CompletableFuture.failedFuture(new WizardStepValidationException(LocalizeValue.of(ScalaCliBundle.message("scala.cli.not.installed"))))
  }
}
