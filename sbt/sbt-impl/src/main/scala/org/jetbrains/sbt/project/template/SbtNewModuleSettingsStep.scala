package org.jetbrains.sbt.project.template

import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.WizardStep
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, ComboBox, Component, UIAccess}
import org.jetbrains.plugins.scala.project.Versions
import org.jetbrains.plugins.scala.project.template.{PackagePrefixStepLike, ScalaSampleCode}
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.api.SbtVersion
import org.jetbrains.sbt.project.template.wizard.ScalaVersionStepLike

import scala.jdk.CollectionConverters.*

/**
 * Selects the sbt and Scala versions of a new sbt project, the package prefix and whether to add sample code.
 * sbt versions older than 1.5 are not offered for Scala 3.
 */
class SbtNewModuleSettingsStep(context: SbtNewModuleContext)
  extends WizardStep[SbtNewModuleContext]
    with ScalaVersionStepLike
    with PackagePrefixStepLike {

  import ScalaVersionStepLike.isScala3VersionString

  private var sbtVersionBox: ComboBox[SbtVersion] = scala.compiletime.uninitialized
  private var sbtVersions: MutableFlatDataModel[SbtVersion] = scala.compiletime.uninitialized
  private var downloadSbtSourcesBox: CheckBox = scala.compiletime.uninitialized
  private var addSampleCodeBox: CheckBox = scala.compiletime.uninitialized

  private var updatingSbtVersions = false
  private var sbtVersionManuallySelected = false

  private val defaultAvailableSbtVersions: Seq[SbtVersion] = Versions.SBT.allHardcodedVersions.map(SbtVersion(_))
  private var availableSbtVersions: Seq[SbtVersion] = defaultAvailableSbtVersions

  @RequiredUIAccess
  override def getComponent(context: SbtNewModuleContext, uiDisposable: Disposable): Component = {
    sbtVersions = FlatDataModel.of(defaultAvailableSbtVersions.asJava)
    sbtVersionBox = ComboBox.create(sbtVersions)
    sbtVersionBox.setTextRenderer(item => if (item == null) LocalizeValue.empty() else LocalizeValue.of(item.minor))
    sbtVersionBox.setValue(context.sbtVersion)
    sbtVersionBox.addValueListener(_ => if (!updatingSbtVersions) sbtVersionManuallySelected = true)

    downloadSbtSourcesBox = CheckBox.create(label("sbt.module.step.download.sources"), context.downloadSbtSources)
    downloadSbtSourcesBox.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.download.sbt.sources")))

    val scalaVersionBox = createScalaVersionBox(context.scalaVersion, uiDisposable)
    val downloadScalaSourcesBox = createDownloadScalaSourcesBox(context.downloadScalaSdkSources)

    ScalaVersionStepLike.downloadVersions(UIAccess.current(), uiDisposable, Versions.SBT.toString)(Versions.SBT.loadVersionsWithProgress(_).map(SbtVersion(_))) { versions =>
      availableSbtVersions = versions
      updateSbtVersions()
    }

    addSampleCodeBox = ScalaSampleCode.createCheckBox()

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.sided(label("sbt.settings.sbt"), versionWithSources(sbtVersionBox, downloadSbtSourcesBox)))
    layout.add(LabeledBuilder.sided(label("sbt.settings.scala"), versionWithSources(scalaVersionBox, downloadScalaSourcesBox)))
    layout.add(createIndentationSyntaxCheckBox())
    layout.add(LabeledBuilder.sided(packagePrefixLabel, createPackagePrefixTextBox(context.packagePrefix)))
    layout.add(addSampleCodeBox)
    selectedScalaVersion.foreach(onScalaVersionChanged)
    layout
  }

  @RequiredUIAccess
  override protected def onScalaVersionChanged(version: String): Unit =
    if (sbtVersionBox != null) updateSbtVersions()

  /**
   * Ensures that sbt versions < 1.5 are not offered if a Scala 3 version is selected.
   */
  @RequiredUIAccess
  private def updateSbtVersions(): Unit = {
    val isScala3Selected = selectedScalaVersion.exists(isScala3VersionString)
    val supported = if (isScala3Selected) Versions.SBT.sbtVersionsForScala3(availableSbtVersions) else availableSbtVersions
    val selected = Option.when(sbtVersionManuallySelected)(sbtVersionBox.getValue)
      .orElse(Versions.SBT.initiallySelectedVersion(supported.map(_.minor)).map(SbtVersion(_)))

    updatingSbtVersions = true
    try replaceVersions(sbtVersionBox, sbtVersions, supported, selected)
    finally updatingSbtVersions = false
  }

  private def versionWithSources(versionBox: Component, downloadSourcesBox: CheckBox): Component = {
    val layout = HorizontalLayout.create()
    layout.add(versionBox)
    layout.add(downloadSourcesBox)
    layout
  }

  @RequiredUIAccess
  override def onStepLeave(context: SbtNewModuleContext): Unit = {
    Option(sbtVersionBox).flatMap(box => Option(box.getValue)).foreach(context.sbtVersion = _)
    selectedScalaVersion.foreach(context.scalaVersion = _)
    Option(downloadSbtSourcesBox).foreach(box => context.downloadSbtSources = box.getValueOrError.booleanValue)
    downloadScalaSources.foreach(context.downloadScalaSdkSources = _)
    context.useIndentationSyntax = useIndentationSyntax
    context.packagePrefix = packagePrefix
    Option(addSampleCodeBox).foreach(box => context.addSampleCode = ScalaSampleCode.isSelected(box))
  }
}
