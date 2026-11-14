package org.jetbrains.plugins.scala.worksheet.settings.ui

import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.ui.Component
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.TabbedLayout
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.settings.ui.WorksheetSettingsPanel.TabTypeData

/** upstream: a Swing panel with JBTabbedPane */
private final class WorksheetAllSettingsTabbedPanel(
  myFile: PsiFile,
  initialFileSettings: WorksheetSettingsData,
  initialDefaultSettings: WorksheetSettingsData,
  availableProfilesProvider: () => Seq[String]
) {

  private def project = myFile.getProject

  private val currentFileSettingsPanel = new WorksheetSettingsPanel(
    TabTypeData.FileSettingsTab(project, myFile.getVirtualFile),
    initialFileSettings,
    availableProfilesProvider
  )

  private val defaultSettingsPanelPanel = new WorksheetSettingsPanel(
    TabTypeData.DefaultProjectSettingsTab(project),
    initialDefaultSettings,
    availableProfilesProvider
  )

  @RequiredUIAccess
  val component: Component = {
    val tabbedLayout = TabbedLayout.create()
    tabbedLayout.addTab(LocalizeValue.of(WorksheetBundle.message("worksheet.settings.panel.settings.for.module", myFile.getName)), currentFileSettingsPanel.component)
    tabbedLayout.addTab(LocalizeValue.of(WorksheetBundle.message("worksheet.settings.panel.default.settings")), defaultSettingsPanelPanel.component)
    tabbedLayout
  }

  def fileSettingsData: WorksheetSettingsData = currentFileSettingsPanel.filledSettingsData
  def defaultProjectSettingsData: WorksheetSettingsData = defaultSettingsPanelPanel.filledSettingsData
}
