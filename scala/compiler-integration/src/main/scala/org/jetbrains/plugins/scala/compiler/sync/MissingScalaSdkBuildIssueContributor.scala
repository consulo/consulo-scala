package org.jetbrains.plugins.scala.compiler.sync

import consulo.annotation.component.ExtensionImpl
import consulo.build.ui.event.MessageEvent
import consulo.build.ui.issue.BuildIssue
import consulo.compiler.BuildIssueContributor
import consulo.localize.LocalizeValue
import consulo.navigation.Navigatable
import consulo.project.Project
import org.jetbrains.plugins.scala.shared.compiler.MissingScalaSdk

import java.nio.file.Path
import scala.jdk.CollectionConverters._

//noinspection ApiStatus,UnstableApiUsage
@ExtensionImpl
final class MissingScalaSdkBuildIssueContributor extends BuildIssueContributor {
  override def createBuildIssue(
    project: Project,
    moduleNames: java.util.Collection[String],
    title: LocalizeValue,
    message: LocalizeValue,
    kind: MessageEvent.Kind,
    file: Path,
    navigatable: Navigatable
  ): BuildIssue = {
    if (!isMissingScalaSdkMessage(message.get, kind)) return null

    val handlers = BuildToolProjectSyncHelper.projectSyncHandlers(project)
    val (externalSystemHandlers, customHandlers) = handlers.partitionMap {
      case ProjectSyncHandler.ExternalSystem => Left(ProjectSyncHandler.ExternalSystem)
      case ProjectSyncHandler.Custom(action) => Right(ProjectSyncHandler.Custom(action))
    }

    val externalSystemSyncAction = if (externalSystemHandlers.nonEmpty) Some(ExternalSystemSyncAction) else None
    val customSyncActions = customHandlers.map(_.syncAction)

    val syncActions = externalSystemSyncAction.toSeq ++ customSyncActions
    val quickFix =
      if (syncActions.nonEmpty) Some(new MissingScalaSdkBuildIssueQuickFix(syncActions))
      else None

    new MissingScalaSdkBuildIssue(moduleNames.asScala.toSeq, quickFix)
  }

  private def isMissingScalaSdkMessage(message: String, kind: MessageEvent.Kind): Boolean =
    kind == MessageEvent.Kind.WARNING &&
      message.startsWith(MissingScalaSdk.MessagePrefix) &&
      message.contains(MissingScalaSdk.SkippedScalaSourcesMessagePrefix)
}
