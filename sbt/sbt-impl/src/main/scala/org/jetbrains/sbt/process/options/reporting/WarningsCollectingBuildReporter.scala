package org.jetbrains.sbt.process.options.reporting

import consulo.build.ui.FilePosition
import consulo.build.ui.issue.BuildIssue
import consulo.navigation.Navigatable
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.build.NoOpBuildReporter

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

private[sbt] final class WarningsCollectingBuildReporter extends NoOpBuildReporter {

  private val warnings = ArrayBuffer.empty[SbtOptionsWarningData]

  def collectedWarnings: Seq[SbtOptionsWarningData] = warnings.toSeq

  override def warning(@Nls message: String, position: Option[FilePosition]): Unit =
    warnings += SbtOptionsWarningData(message, message)

  override def warning(@Nls message: String, position: Option[FilePosition], @Nls details: String): Unit =
    warnings += SbtOptionsWarningData(message, details)

  override def warning(issue: BuildIssue): Unit =
    warnings += SbtOptionsWarningData(issue.getTitle.get, issue.getDescription.get, issue.getQuickFixes.asScala.toSeq)

  override def warning(
    @Nls message: String,
    position: Option[FilePosition],
    @Nls details: String,
    navigatable: Option[Navigatable]
  ): Unit =
    warnings += SbtOptionsWarningData(message, details)
}
