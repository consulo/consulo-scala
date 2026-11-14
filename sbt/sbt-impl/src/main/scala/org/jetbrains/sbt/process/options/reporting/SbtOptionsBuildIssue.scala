package org.jetbrains.sbt.process.options.reporting

import consulo.build.ui.issue.BuildIssue
import consulo.build.ui.issue.BuildIssueQuickFix
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.navigation.Navigatable

import java.util.{List => JList}
import scala.jdk.CollectionConverters.*

private final class SbtOptionsBuildIssue(warning: SbtOptionsWarningData) extends BuildIssue {
  override def getTitle: LocalizeValue = LocalizeValue.of(warning.title)

  override def getDescription: LocalizeValue = LocalizeValue.of(warning.details)

  override def getQuickFixes: JList[BuildIssueQuickFix] = warning.quickFixes.asJava

  override def getNavigatable(project: Project): Navigatable = null
}
