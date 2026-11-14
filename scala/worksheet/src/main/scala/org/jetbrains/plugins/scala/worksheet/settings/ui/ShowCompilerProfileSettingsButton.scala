package org.jetbrains.plugins.scala.worksheet.settings.ui

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ide.setting.ShowSettingsUtil
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.{Button, UIAccess}
import org.jetbrains.plugins.scala.project.settings.{ScalaCompilerConfigurable, ScalaCompilerProfilesPanel}
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle

/**
 * upstream: an AnAction shown as ActionButton, which opens the Settings dialog with the "Scala Compiler" page only
 * (ShowSettingsUtilImpl.getDialog), here the Settings dialog opens at the "Scala Compiler" page
 */
private object ShowCompilerProfileSettingsButton {

  @RequiredUIAccess
  def create(
    project: Project,
    selectedProfileProvider: () => String,
    profilesUpdatedListener: () => Unit
  ): Button = {
    val button = Button.create(LocalizeValue.empty())
    button.setIcon(PlatformIconGroup.generalSettings())
    button.setToolTipText(LocalizeValue.of(WorksheetBundle.message("worksheet.show.compiler.profiles.settings")))
    button.addClickListener { _ =>
      val uiAccess = UIAccess.current()
      val selectedProfile = Option(selectedProfileProvider())
      val future = ScalaCompilerProfilesPanel.withTemporarySelectedProfile(project, selectedProfile) {
        ShowSettingsUtil.getInstance.showSettingsDialog(project, ScalaCompilerConfigurable.Id, "")
      }
      future.whenComplete((_, _) => uiAccess.give(() => profilesUpdatedListener()))
    }
    button
  }
}
