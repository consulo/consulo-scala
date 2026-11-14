package org.jetbrains.scalaCli.project.template.wizard

import consulo.application.ApplicationManager
import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.{WizardStep, WizardStepValidationException}
import consulo.ui.layout.VerticalLayout
import consulo.ui.util.LabeledBuilder
import consulo.ui.{ComboBox, Component}
import consulo.util.lang.SystemProperties
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.scalaCli.{ScalaCliBundle, ScalaCliUtils}

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*

/**
 * Selects the Scala version of a new Scala CLI project and checks that Scala CLI is installed.
 */
// upstream: ScalaCliNewProjectWizardStep.setupUI (IntelliJ Kotlin UI DSL panel with the versions downloaded from the network)
class ScalaCliNewModuleSettingsStep extends WizardStep[ScalaCliNewModuleContext] {

  private var scalaVersionBox: ComboBox[String] = scala.compiletime.uninitialized

  @RequiredUIAccess
  override def getComponent(context: ScalaCliNewModuleContext, uiDisposable: Disposable): Component = {
    scalaVersionBox = ComboBox.create(ScalaCliNewModuleSettingsStep.ScalaVersions.asJava)
    scalaVersionBox.setValue(context.scalaVersion)

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Scala"), scalaVersionBox))
    layout
  }

  override def onStepLeave(context: ScalaCliNewModuleContext): Unit =
    Option(scalaVersionBox).flatMap(box => Option(box.getValue)).foreach(context.scalaVersion = _)

  @RequiredUIAccess
  override def validateStep(context: ScalaCliNewModuleContext): CompletableFuture[?] = {
    // upstream: the installation is checked in the user home (in the project directory in tests)
    val installationPath = Path.of(SystemProperties.getUserHome)
    if (ApplicationManager.getApplication.isUnitTestMode || ScalaCliUtils.isScalaCliInstalled(installationPath))
      CompletableFuture.completedFuture(null)
    else
      CompletableFuture.failedFuture(new WizardStepValidationException(LocalizeValue.of(ScalaCliBundle.message("scala.cli.not.installed"))))
  }
}

object ScalaCliNewModuleSettingsStep {
  // upstream: Versions.Scala.allHardcodedVersions filtered by the minimal supported Scala 3 version
  private val ScalaVersions: Seq[String] = Seq(
    ScalaVersion.Latest.Scala_2_13,
    ScalaVersion.Latest.Scala_2_12,
    ScalaVersion.Latest.Scala_3_LTS,
    ScalaVersion.Latest.Scala_3_8
  ).map(_.minor).distinct
}
