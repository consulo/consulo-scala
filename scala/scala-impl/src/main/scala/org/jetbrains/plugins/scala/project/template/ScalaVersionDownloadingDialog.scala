package org.jetbrains.plugins.scala.project.template

import consulo.component.ProcessCanceledException
import consulo.application.progress.ProgressIndicator
import consulo.ui.ex.awt.Messages
import scala.annotation.nowarn
import org.apache.ivy.util.MessageLogger
import org.jetbrains.plugins.scala.components.libextensions.ProgressIndicatorLogger
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, withProgressSynchronouslyTry}
import org.jetbrains.plugins.scala.project.external.ScalaSdkUtils
import org.jetbrains.plugins.scala.project.template.Artifact.ScalaLibrary
import org.jetbrains.plugins.scala.project.template.ScalaVersionDownloadingDialog.{ScalaVersionResolveResult, createScalaVersionResolveResult, preselectLatestStableScalaVersion}
import org.jetbrains.plugins.scala.project.{ReplClasspath, Versions}
import org.jetbrains.plugins.scala.rt.project.Version
import org.jetbrains.plugins.scala.{DependencyManagerBase, ScalaBundle, ScalaVersion}
import consulo.localize.LocalizeValue
import consulo.ui.ComboBox

import java.nio.file.Path
import javax.swing.JComponent
import scala.jdk.CollectionConverters._
import scala.util.Try

final class ScalaVersionDownloadingDialog(parent: JComponent) extends VersionDialogBase(parent) {

  {
    init()

    setTitle(ScalaBundle.message("title.download"))
    myVersion.setTextRenderer(version => LocalizeValue.of(if (version == null) "" else Version.abbreviate(version)))

    val versions = Versions.Scala.loadVersionsWithProgressDialog()
    if (versions.nonEmpty) {
      val stringRepresentation = versions.map(_.presentation)
      myVersions.replaceAll(stringRepresentation.asJava)
      preselectLatestStableScalaVersion(myVersion, stringRepresentation)
    }
    else
      Messages.showErrorDialog(
        createCenterPanel(),
        ScalaBundle.message("no.versions.available.for.download"),
        ScalaBundle.message("title.error.downloading.scala.libraries")
      )
  }

  def showAndGetSelected(): Option[ScalaVersionResolveResult] =
    if (showAndGet()) {
      val scalaVersionStr = myVersion.getValue
      val scalaVersion = ScalaVersion.fromString(scalaVersionStr).getOrElse {
        Messages.showErrorDialog(
          parent,
          ScalaBundle.message("invalid.scala.version.format", scalaVersionStr),
          ScalaBundle.message("error.downloading.scala.version", scalaVersionStr)
        )
        return None
      }

      val tri: Try[ScalaVersionResolveResult] = withProgressSynchronouslyTry(ScalaBundle.message("downloading.scala.version", scalaVersionStr), canBeCanceled = true) { manager =>
        val indicator = manager.getProgressIndicator
        val dependencyManager = new DependencyManagerBase {
          override protected def progressIndicator: Option[ProgressIndicator] = Some(indicator)
          override def createLogger: MessageLogger = new ProgressIndicatorLogger(indicator)
        }
        createScalaVersionResolveResult(scalaVersion, dependencyManager)
      }
      tri.fold({
        case _: ProcessCanceledException =>
          None
        case exception =>
          //noinspection ReferencePassedToNls
          Messages.showErrorDialog(
            parent,
            exception.getMessage,
            ScalaBundle.message("error.downloading.scala.version", scalaVersionStr)
          )
          None
      }, Some(_))
    }
    else None
}

object ScalaVersionDownloadingDialog {

  final case class ScalaVersionResolveResult(
    scalaVersion: String,
    compilerClassPathJars: Seq[Path],
    librarySourcesJars: Seq[Path],
    compilerBridgeJar: Option[Path],
    replClasspath: ReplClasspath
  )

  /**
   * Preselects the latest stable Scala version in the given combo box - skipping RC versions.
   */
  private def preselectLatestStableScalaVersion(versionComboBox: ComboBox[String], versions: Seq[String]): Unit = {
    val selectIndex = versions.indexWhere(v => !v.contains("RC"))
    if (selectIndex >= 0) {
      // upstream: also scrolls the Swing popup list to the top to keep the newest versions visible
      versionComboBox.setValue(versions(selectIndex))
    }
  }

  // upstream: @VisibleForTesting
  def createScalaVersionResolveResult(scalaVersion: ScalaVersion, dependencyManager: DependencyManagerBase): ScalaVersionResolveResult = {
    import org.jetbrains.plugins.scala.DependencyManagerBase._

    val (compiler, librarySources) = (
      DependencyDescription.scalaArtifact("compiler", scalaVersion).transitive(),
      // scala3-library is not the reason why "transitive" should be added in "library" kind,
      // because scala2-library is already downloaded from "compiler" kind (as a result of the fact that there is a transitive download)
      DependencyDescription.scalaArtifact("library", scalaVersion).sources(),
    )
    val compilerClasspathResolveResult = dependencyManager.resolve(compiler)
    // ATTENTION:
    // Sources jars should be resolved in a separate request to dependency manager
    // Otherwise resolved jars will contain only "sources" jar, without "classes"
    val librarySourcesResolveResult = dependencyManager.resolve(librarySources)

    def getScala2LibrarySources: Seq[ResolvedDependency] = {
      val scala2Library = compilerClasspathResolveResult.filter(_.file.getFileName.toString.startsWith(ScalaLibrary.prefix))
      val scala2VersionOpt = scala2Library.headOption.flatMap { rd => ScalaVersion.fromString(rd.info.version) }
      scala2VersionOpt.map { scala2Version =>
        val scala2LibrarySources = DependencyDescription.scalaArtifact("library", scala2Version).sources()
        dependencyManager.resolve(scala2LibrarySources)
      }.getOrElse(Seq.empty)
    }

    // It is necessary to download scala2-library sources explicitly.
    // At least I couldn't find a way to force Ivy to download sources of transitive dependencies.
    val scala2LibraryDependency =
      if (scalaVersion.isScala3) getScala2LibrarySources
      else Seq.empty

    val compilerBridge = ScalaSdkUtils.resolveCompilerBridgeJar(scalaVersion.minor): @nowarn("msg=deprecated")

    val replClasspath = ScalaSdkUtils.resolveReplClasspath(scalaVersion.minor): @nowarn("msg=deprecated")

    ScalaVersionResolveResult(
      scalaVersion.minor,
      compilerClasspathResolveResult.map(_.file),
      (librarySourcesResolveResult ++ scala2LibraryDependency).map(_.file),
      compilerBridge,
      replClasspath
    )
  }
}
