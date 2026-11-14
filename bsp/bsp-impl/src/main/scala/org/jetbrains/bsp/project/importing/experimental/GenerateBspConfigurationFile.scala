package org.jetbrains.bsp.project.importing.experimental

import consulo.annotation.component.ActionImpl

import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.bsp.{BspBundle, BspUtil}

// TODO SCL-24897
@ActionImpl(id = "BSP.RegenerateBspConfigurationFile")
class GenerateBspConfigurationFile extends AnAction(
  LocalizeValue.of(BspBundle.message("generate.bsp.configuration.file"))
) with AnActionWithSyncUpdate {

  override def update(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    val isBspProject = project != null && BspUtil.isBspProject(project)
    if (!isBspProject) {
      e.getPresentation.setEnabledAndVisible(false)
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project == null)
      return

    // upstream: ProjectUtil.guessProjectDir
    val projectDir = project.getBaseDir
    if (projectDir == null)
      return

    val workspace = projectDir.toNioPath
    new GenerateBspConfig(project, workspace).runSynchronously()
  }
}
