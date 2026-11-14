package org.jetbrains.sbt.project

import consulo.fileEditor.EditorNotificationBuilder
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.sbt.SbtBundle

// upstream: builds an EditorNotificationPanel
private object SbtProjectImportStateNotificationPanel {
  def fillNotification(project: Project, builder: EditorNotificationBuilder): EditorNotificationBuilder = {
    builder.asWarning()
    builder.withText(LocalizeValue.of(SbtBundle.message("sbt.project.not.loaded.notification.panel.text")))
    builder.withAction(LocalizeValue.of(SbtBundle.message("sbt.project.not.loaded.notification.panel.action.label")), _ =>
      SbtExternalSystemUtil.loadSbtProject(project)
    )
    builder
  }
}
