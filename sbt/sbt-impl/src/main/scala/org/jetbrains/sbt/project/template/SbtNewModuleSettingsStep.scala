package org.jetbrains.sbt.project.template

import consulo.application.Application
import consulo.application.progress.{EmptyProgressIndicator, ProgressIndicator, ProgressManager}
import consulo.component.ProcessCanceledException
import consulo.disposer.{Disposable, Disposer}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.WizardStep
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, ComboBox, Component, UIAccess}
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.project.Versions
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.api.SbtVersion

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/**
 * Selects the sbt and Scala versions of a new sbt project.
 */
// upstream: SbtScalaNewProjectWizardStep (IntelliJ new project wizard, AsynchronousVersionsDownloading): the latest known
// versions are offered first and replaced by the versions downloaded in the background; package prefix is not supported
class SbtNewModuleSettingsStep(context: SbtNewModuleContext) extends WizardStep[SbtNewModuleContext] {

  import SbtNewModuleSettingsStep.*

  private var sbtVersionBox: ComboBox[SbtVersion] = _
  private var scalaVersionBox: ComboBox[String] = _
  private var downloadSbtSourcesBox: CheckBox = _
  private var downloadScalaSourcesBox: CheckBox = _

  private var updatingVersions = false
  private var sbtVersionManuallySelected = false
  private var scalaVersionManuallySelected = false

  @RequiredUIAccess
  override def getComponent(context: SbtNewModuleContext, uiDisposable: Disposable): Component = {
    val sbtVersions = FlatDataModel.of(DefaultSbtVersions.asJava)
    sbtVersionBox = ComboBox.create(sbtVersions)
    sbtVersionBox.setTextRenderer(item => if (item == null) LocalizeValue.empty() else LocalizeValue.of(item.minor))
    sbtVersionBox.setValue(context.sbtVersion)
    sbtVersionBox.addValueListener(_ => if (!updatingVersions) sbtVersionManuallySelected = true)

    val scalaVersions = FlatDataModel.of(DefaultScalaVersions.asJava)
    scalaVersionBox = ComboBox.create(scalaVersions)
    scalaVersionBox.setValue(context.scalaVersion)
    scalaVersionBox.addValueListener(_ => if (!updatingVersions) scalaVersionManuallySelected = true)

    downloadSbtSourcesBox = CheckBox.create(label("sbt.module.step.download.sources"), context.downloadSbtSources)
    downloadSbtSourcesBox.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.download.sbt.sources")))
    downloadScalaSourcesBox = CheckBox.create(label("sbt.module.step.download.sources"), context.downloadScalaSdkSources)
    downloadScalaSourcesBox.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.download.scala.standard.library.sources")))

    val uiAccess = UIAccess.current()
    downloadVersions(uiAccess, uiDisposable, Versions.SBT.toString)(Versions.SBT.loadVersionsWithProgress(_).map(SbtVersion(_))) { versions =>
      val selected = Option.when(sbtVersionManuallySelected)(sbtVersionBox.getValue)
        .orElse(Versions.SBT.initiallySelectedVersion(versions.map(_.minor)).map(SbtVersion(_)))
      replaceVersions(sbtVersionBox, sbtVersions, versions, selected)
    }
    downloadVersions(uiAccess, uiDisposable, Versions.Scala.toString)(Versions.Scala.loadVersionsWithProgress(_).map(_.presentation)) { versions =>
      val selected = Option.when(scalaVersionManuallySelected)(scalaVersionBox.getValue)
        .orElse(Versions.Scala.initiallySelectedVersion(versions))
      replaceVersions(scalaVersionBox, scalaVersions, versions, selected)
    }

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.sided(label("sbt.settings.sbt"), versionWithSources(sbtVersionBox, downloadSbtSourcesBox)))
    layout.add(LabeledBuilder.sided(label("sbt.settings.scala"), versionWithSources(scalaVersionBox, downloadScalaSourcesBox)))
    layout
  }

  private def versionWithSources(versionBox: Component, downloadSourcesBox: CheckBox): Component = {
    val layout = HorizontalLayout.create()
    layout.add(versionBox)
    layout.add(downloadSourcesBox)
    layout
  }

  @RequiredUIAccess
  private def replaceVersions[T](box: ComboBox[T], model: MutableFlatDataModel[T], versions: Seq[T], selected: Option[T]): Unit =
    if (versions.nonEmpty) {
      updatingVersions = true
      try {
        val previous = box.getValue
        model.replaceAll(versions.asJava)
        selected.orElse(Option(previous)).filter(versions.contains).orElse(versions.headOption).foreach(box.setValue)
      } finally updatingVersions = false
    }

  private def label(key: String): LocalizeValue =
    LocalizeValue.of(SbtBundle.message(key).replace("&", ""))

  override def onStepLeave(context: SbtNewModuleContext): Unit = {
    Option(sbtVersionBox).flatMap(box => Option(box.getValue)).foreach(context.sbtVersion = _)
    Option(scalaVersionBox).flatMap(box => Option(box.getValue)).foreach(context.scalaVersion = _)
    Option(downloadSbtSourcesBox).foreach(box => context.downloadSbtSources = box.getValueOrError.booleanValue)
    Option(downloadScalaSourcesBox).foreach(box => context.downloadScalaSdkSources = box.getValueOrError.booleanValue)
  }
}

object SbtNewModuleSettingsStep {
  private val Log = Logger.getInstance(classOf[SbtNewModuleSettingsStep])

  private val DefaultSbtVersions: Seq[SbtVersion] = Seq(SbtVersion.Latest.Sbt_1, SbtVersion.Latest.Sbt_2).distinct

  private val DefaultScalaVersions: Seq[String] = Seq(
    ScalaVersion.Latest.Scala_2_13,
    ScalaVersion.Latest.Scala_2_12,
    ScalaVersion.Latest.Scala_3_LTS,
    ScalaVersion.Latest.Scala_3_8
  ).map(_.minor).distinct

  // upstream: AsynchronousVersionsDownloading.downloadVersionsAsynchronously
  private def downloadVersions[V](uiAccess: UIAccess, uiDisposable: Disposable, versionType: String)
                                 (load: ProgressIndicator => Seq[V])
                                 (onLoaded: Seq[V] => Unit): Unit = {
    val indicator = new EmptyProgressIndicator
    Disposer.register(uiDisposable, () => indicator.cancel())
    Application.get().executeOnPooledThread((() => {
      Try(ProgressManager.getInstance().runProcess(() => load(indicator).sorted(using versionOrdering[V]).reverse, indicator)) match {
        case Success(versions) =>
          uiAccess.give(() => if (!indicator.isCanceled) onLoaded(versions))
        case Failure(_: ProcessCanceledException) =>
        case Failure(exception) =>
          Log.debug(s"Exception during downloading of $versionType versions", exception)
      }
    }): Runnable)
  }

  private def versionOrdering[V]: Ordering[V] = Ordering.by[V, org.jetbrains.plugins.scala.rt.project.Version] {
    case version: SbtVersion => version.value
    case version => org.jetbrains.plugins.scala.rt.project.Version(version.toString)
  }
}
