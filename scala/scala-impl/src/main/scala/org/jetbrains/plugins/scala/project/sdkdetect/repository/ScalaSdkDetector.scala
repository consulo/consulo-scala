package org.jetbrains.plugins.scala.project.sdkdetect.repository

import consulo.application.progress.ProgressIndicator
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.project.template._

trait ScalaSdkDetector {

  @Nls
  def friendlyName: String

  def collectSdkChoices(implicit indicator: ProgressIndicator): Seq[SdkChoice]
}

object ScalaSdkDetector {
  def allDetectors(contextDirectory: VirtualFile): Seq[ScalaSdkDetectorBase] =
    Seq(
      new ProjectLocalDetector(contextDirectory),
      SystemDetector,
      BrewDetector,
      IvyDetector,
      MavenDetector,
      SdkmanDetector,
      CoursierDetector
    )
}
