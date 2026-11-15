package org.jetbrains.sbt.project.template.wizard

import consulo.application.Application
import consulo.application.progress.{EmptyProgressIndicator, ProgressIndicator, ProgressManager}
import consulo.component.ProcessCanceledException
import consulo.disposer.{Disposable, Disposer}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.{CheckBox, ComboBox, UIAccess}
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.project.Versions
import org.jetbrains.plugins.scala.project.template.IndentationSyntaxStepLike
import org.jetbrains.plugins.scala.rt.project.Version
import org.jetbrains.sbt.SbtBundle

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/**
 * The Scala version of a new project: the hardcoded versions are offered first and replaced by the versions downloaded
 * in the background; the indentation syntax check box is shown for Scala 3 versions.
 */
trait ScalaVersionStepLike extends IndentationSyntaxStepLike {

  import ScalaVersionStepLike.*

  private var scalaVersionBox: ComboBox[String] = scala.compiletime.uninitialized
  private var scalaVersions: MutableFlatDataModel[String] = scala.compiletime.uninitialized
  private var downloadScalaSourcesBox: CheckBox = scala.compiletime.uninitialized

  private var updatingScalaVersions = false
  private var scalaVersionManuallySelected = false

  protected def defaultAvailableScalaVersions: Seq[String] =
    filterScala2OrSupportedScala3Versions(Versions.Scala.allHardcodedVersions.map(_.presentation), MinSupportedScala3Version)

  /** called with the selected Scala version whenever it changes */
  @RequiredUIAccess
  protected def onScalaVersionChanged(version: String): Unit = {}

  @RequiredUIAccess
  protected final def createScalaVersionBox(initial: String, uiDisposable: Disposable): ComboBox[String] = {
    scalaVersions = FlatDataModel.of(defaultAvailableScalaVersions.asJava)
    scalaVersionBox = ComboBox.create(scalaVersions)
    scalaVersionBox.setValue(if (defaultAvailableScalaVersions.contains(initial)) initial else defaultAvailableScalaVersions.head)
    scalaVersionBox.addValueListener { event =>
      if (!updatingScalaVersions) scalaVersionManuallySelected = true
      Option(event.getValue).foreach(scalaVersionChanged)
    }
    Option(scalaVersionBox.getValue).foreach(scalaVersionChanged)

    downloadVersions(UIAccess.current(), uiDisposable, Versions.Scala.toString)(Versions.Scala.loadVersionsWithProgress(_).map(_.presentation)) { loaded =>
      val versions = filterScala2OrSupportedScala3Versions(loaded, MinSupportedScala3Version)
      val selected = Option.when(scalaVersionManuallySelected)(scalaVersionBox.getValue)
        .orElse(Versions.Scala.initiallySelectedVersion(versions))
      replaceVersions(scalaVersionBox, scalaVersions, versions, selected)
    }
    scalaVersionBox
  }

  @RequiredUIAccess
  protected final def createDownloadScalaSourcesBox(initial: Boolean): CheckBox = {
    downloadScalaSourcesBox = CheckBox.create(label("sbt.module.step.download.sources"), initial)
    downloadScalaSourcesBox.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.download.scala.standard.library.sources")))
    downloadScalaSourcesBox
  }

  protected final def selectedScalaVersion: Option[String] =
    Option(scalaVersionBox).flatMap(box => Option(box.getValue))

  protected final def downloadScalaSources: Option[Boolean] =
    Option(downloadScalaSourcesBox).map(_.getValueOrError.booleanValue)

  @RequiredUIAccess
  private def scalaVersionChanged(version: String): Unit = {
    setShowIndentationSyntaxCheckBox(isScala3VersionString(version))
    onScalaVersionChanged(version)
  }

  /**
   * Replaces the items of a version combo box keeping the selected version if it is still available.
   */
  @RequiredUIAccess
  protected final def replaceVersions[T](box: ComboBox[T], model: MutableFlatDataModel[T], versions: Seq[T], selected: Option[T]): Unit =
    if (versions.nonEmpty) {
      val wasUpdating = updatingScalaVersions
      updatingScalaVersions = true
      try {
        val previous = box.getValue
        model.replaceAll(versions.asJava)
        selected.orElse(Option(previous)).filter(versions.contains).orElse(versions.headOption).foreach(box.setValue)
      } finally updatingScalaVersions = wasUpdating
    }

  protected final def label(key: String): LocalizeValue =
    LocalizeValue.of(SbtBundle.message(key).replace("&", ""))
}

object ScalaVersionStepLike {
  private val Log = Logger.getInstance(classOf[ScalaVersionStepLike])

  // See SCL-22032
  val MinSupportedScala3Version: ScalaVersion = ScalaVersion.Latest.Scala_3_3.withMinor(0)

  def filterScala2OrSupportedScala3Versions(scalaVersions: Seq[String], minSupportedScala3Version: ScalaVersion): Seq[String] =
    scalaVersions.filter { scalaVersion =>
      ScalaVersion.fromString(scalaVersion).forall(isScala2OrSupportedScala3Version(_, minSupportedScala3Version))
    }

  private def isScala2OrSupportedScala3Version(scalaVersion: ScalaVersion, minSupportedScala3Version: ScalaVersion): Boolean =
    scalaVersion.isScala2 || scalaVersion >= minSupportedScala3Version

  def isSupportedScala3Version(scalaVersion: ScalaVersion, minSupportedScala3Version: ScalaVersion): Boolean =
    scalaVersion.isScala3 && scalaVersion >= minSupportedScala3Version

  def isScala3VersionString(str: String): Boolean =
    Version(str) >= Version("3.0.0")

  /**
   * Loads the versions in the background, `onLoaded` gets them sorted from the newest one, in the UI thread.
   */
  def downloadVersions[V](uiAccess: UIAccess, uiDisposable: Disposable, versionType: String)
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

  private def versionOrdering[V]: Ordering[V] = Ordering.by[V, Version] {
    case version: org.jetbrains.sbt.api.SbtVersion => version.value
    case version => Version(version.toString)
  }
}
