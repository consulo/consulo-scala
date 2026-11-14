package org.jetbrains.plugins.scala.compiler.sync

import consulo.build.ui.issue.BuildIssueQuickFix
import consulo.dataContext.{DataContext, DataProvider}
import consulo.project.Project
import consulo.util.dataholder.Key

import java.util.concurrent.CompletableFuture

//noinspection ApiStatus,UnstableApiUsage
private final class MissingScalaSdkBuildIssueQuickFix(syncActions: Seq[SyncAction]) extends BuildIssueQuickFix {
  override def getId: String = MissingScalaSdkBuildIssueQuickFix.ID

  override def runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture[?] = {
    val dataContext = MissingScalaSdkBuildIssueQuickFix.dataContext(project, dataProvider)
    syncActions.foreach(_.syncProject(project, dataContext))
    CompletableFuture.completedFuture(null)
  }
}

private object MissingScalaSdkBuildIssueQuickFix {
  final val ID = "missing.scala.sdk.build.issue.quick.fix"

  // the build view gives a DataProvider which may not know the project
  private def dataContext(project: Project, dataProvider: DataProvider): DataContext = new DataContext {
    override def getData[T](key: Key[T]): T = {
      val data = dataProvider.getDataUnchecked(key)
      if ((data == null) && (key == Project.KEY)) project.asInstanceOf[T] else data
    }
  }
}
