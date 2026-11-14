package org.jetbrains.sbt.process.options.reporting

import consulo.build.ui.event.BuildEventsNls
import consulo.build.ui.issue.BuildIssueQuickFix
//noinspection ApiStatus,UnstableApiUsage
private[sbt] final case class SbtOptionsWarningData(
  @BuildEventsNls.Title
  title: String,
  @BuildEventsNls.Description
  details: String,
  quickFixes: Seq[BuildIssueQuickFix] = Seq.empty,
)
