package org.jetbrains.sbt.project.modifier.ui

import consulo.disposer.Disposable
import consulo.ui.ex.action.{ActionManager, IdeActions}
import consulo.project.{Project => IJProject}
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.DialogWrapper
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.versionControlSystem.change.Change
import consulo.versionControlSystem.ui.{VcsChangesBrowser, VcsChangesDialogFactory}
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.sbt.SbtBundle

import javax.swing.JComponent
import scala.collection.mutable
import scala.jdk.CollectionConverters._


/**
 * A confirmation dialog for build file changes. Uses basic vcs components to display difference between changed copy
 * and original build file. Notice that a trick is used to display proper paths in changes tree view: changes have
 * 'before/'after' revisions switch places so that in tree view path to 'before' is used while in diff view revisions
 * are in default places.
 */
// upstream: BuildFileChangeBrowser (a SimpleAsyncChangesBrowser) - Consulo's VcsChangesBrowser is used; the statuses of
// manually modified build files are not refreshed (fileStatusMap is not updated)
class ChangesConfirmationDialog private (val project: IJProject, private val changes: List[BuildFileChange],
                                         val fileStatusMap: mutable.Map[VirtualFile, (BuildFileModifiedStatus, Long)]) extends DialogWrapper(project) {

  private var myChangesBrowser: VcsChangesBrowser = scala.compiletime.uninitialized

  setTitle(SbtBundle.message("sbt.build.file.changes"))
  init()

  def buildFileChanges: Seq[BuildFileChange] = {
    val changes = myChangesBrowser.getChanges.asScala.map(_.asInstanceOf[BuildFileChange])
    changes.map(BuildFileChange.swap).toSeq
  }

  @RequiredUIAccess
  override def createCenterPanel(): JComponent = {
    val swappedChanges: java.util.List[Change] = new java.util.ArrayList[Change]()
    swappedChanges.addAll(changes.map(BuildFileChange.swap).asJavaCollection)

    val changesBrowser = project.getApplication.getInstance(classOf[VcsChangesDialogFactory])
      .changesBrowser(project)
      .toolbarAction(ActionManager.getInstance.getAction(IdeActions.ACTION_EDIT_SOURCE))
      .build(getDisposable: Disposable)
    changesBrowser.setChanges(swappedChanges)

    myChangesBrowser = changesBrowser

    TargetAWT.to(changesBrowser.getComponent).asInstanceOf[JComponent]
  }

}

object ChangesConfirmationDialog {
  def apply(project: IJProject, changes: List[BuildFileChange], fileChangesMap: mutable.Map[VirtualFile, (BuildFileModifiedStatus, Long)]): ChangesConfirmationDialog =
    new ChangesConfirmationDialog(project, changes, fileChangesMap)
}
