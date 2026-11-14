package org.jetbrains.sbt.project

import consulo.externalSystem.importing.ImportSpecBuilder
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher
import consulo.project.Project
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.settings.SbtSettings

private object SbtExternalSystemUtil {

  def loadSbtProject(project: Project): Unit = {
    val externalProjectPath = SbtUtil.getWorkingDirPath(project)
    // upstream: SbtUnlinkedProjectAware.isLinkedProject / linkAndLoadProjectAsync (external system autolink, not in Consulo)
    if (SbtSettings.getInstance(project).getLinkedProjectSettings(externalProjectPath) != null) {
      // The project was already imported at some point in the past. For the ExternalSystem machinery, it is
      // considered "linked" and can simply be refreshed.
      val builder = new ImportSpecBuilder(project, SbtProjectSystem.Id).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      ExternalSystemProjectRefresher.getInstance().refreshProjects(builder)
    } else {
      // The project has never been imported. It needs to be both loaded and linked.
      new SbtOpenProjectProvider().linkToExistingProject(externalProjectPath, project)
    }
  }
}
