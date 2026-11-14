package org.jetbrains.plugins.scala.worksheet.settings.ui

import consulo.module.Module
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetExternalRunType

private case class WorksheetSettingsData(
  isInteractive: Boolean,
  isMakeBeforeRun: Boolean,
  runType: WorksheetExternalRunType,
  cpModule: Module,
  compilerProfile: String
)
