package org.jetbrains.sbt.project.autolink

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.project.Project

@ExtensionAPI(ComponentScope.APPLICATION)
trait SbtUnlinkedProjectAwareHelper {

  /**
   * @return true when the project which looks like sbt project (i.e. has `build.sbt` file)
   *         is already imported as some other type of project (e.g. as BSP project, which uses SBT as a server)
   */
  def isLinkedProject(project: Project, externalProjectPath: String): Boolean
}

object SbtUnlinkedProjectAwareHelper {

  def isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    Application.get().getExtensionPoint(classOf[SbtUnlinkedProjectAwareHelper])
      .anyMatchSafe(_.isLinkedProject(project, externalProjectPath))
}
