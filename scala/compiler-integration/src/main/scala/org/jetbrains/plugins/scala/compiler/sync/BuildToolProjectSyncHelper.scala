package org.jetbrains.plugins.scala.compiler.sync

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.project.Project

import scala.jdk.CollectionConverters._

@ExtensionAPI(ComponentScope.APPLICATION)
trait BuildToolProjectSyncHelper {
  def projectSyncHandler(project: Project): Option[ProjectSyncHandler]
}

object BuildToolProjectSyncHelper {
  def projectSyncHandlers(project: Project): Seq[ProjectSyncHandler] =
    Application.get().getExtensionPoint(classOf[BuildToolProjectSyncHelper]).getExtensionList.asScala.toSeq.flatMap(_.projectSyncHandler(project))
}
