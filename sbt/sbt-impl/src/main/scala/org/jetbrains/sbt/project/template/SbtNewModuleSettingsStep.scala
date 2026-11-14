package org.jetbrains.sbt.project.template

import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.WizardStep
import consulo.ui.layout.VerticalLayout
import consulo.ui.util.LabeledBuilder
import consulo.ui.{ComboBox, Component}
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.api.SbtVersion

import scala.jdk.CollectionConverters.*

/**
 * Selects the sbt and Scala versions of a new sbt project.
 */
// upstream: SbtScalaNewProjectWizardStep (IntelliJ new project wizard); the versions are not downloaded,
// the latest known versions are offered
class SbtNewModuleSettingsStep(context: SbtNewModuleContext) extends WizardStep[SbtNewModuleContext] {

  private var sbtVersionBox: ComboBox[SbtVersion] = _
  private var scalaVersionBox: ComboBox[String] = _

  @RequiredUIAccess
  override def getComponent(context: SbtNewModuleContext, uiDisposable: Disposable): Component = {
    sbtVersionBox = ComboBox.create(SbtNewModuleSettingsStep.SbtVersions.asJava)
    sbtVersionBox.setTextRenderer(item => if (item == null) LocalizeValue.empty() else LocalizeValue.of(item.minor))
    sbtVersionBox.setValue(context.sbtVersion)

    scalaVersionBox = ComboBox.create(SbtNewModuleSettingsStep.ScalaVersions.asJava)
    scalaVersionBox.setValue(context.scalaVersion)

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.sided(label("sbt.settings.sbt"), sbtVersionBox))
    layout.add(LabeledBuilder.sided(label("sbt.settings.scala"), scalaVersionBox))
    layout
  }

  private def label(key: String): LocalizeValue =
    LocalizeValue.of(SbtBundle.message(key).replace("&", ""))

  override def onStepLeave(context: SbtNewModuleContext): Unit = {
    Option(sbtVersionBox).flatMap(box => Option(box.getValue)).foreach(context.sbtVersion = _)
    Option(scalaVersionBox).flatMap(box => Option(box.getValue)).foreach(context.scalaVersion = _)
  }
}

object SbtNewModuleSettingsStep {
  private val SbtVersions: Seq[SbtVersion] = Seq(SbtVersion.Latest.Sbt_1, SbtVersion.Latest.Sbt_2).distinct

  private val ScalaVersions: Seq[String] = Seq(
    ScalaVersion.Latest.Scala_2_13,
    ScalaVersion.Latest.Scala_2_12,
    ScalaVersion.Latest.Scala_3_LTS,
    ScalaVersion.Latest.Scala_3_8
  ).map(_.minor).distinct
}
