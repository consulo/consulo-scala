package org.jetbrains.plugins.scala.lang.transformation

import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.language.editor.CommonDataKeys
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.actions.ScalaActionUtil
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.statistics.ScalaActionUsagesCollector

class DesugarCodeAction extends AnAction(
  ScalaBundle.message("desugar.scala.code.action.text"),
  ScalaBundle.message("desugar.scala.code.action.description"),
  /* icon = */ null
) with AnActionWithSyncUpdate {
  // TODO support read-only files (create duplicate scratch buffer)
  override def actionPerformed(event: AnActionEvent): Unit = {
    ScalaActionUsagesCollector.logDesugarCode(event.getData(Project.KEY))

    implicit val project: Project = event.getData(Project.KEY)
    if (project == null) return

    val file = event.getDataContext.getData(CommonDataKeys.PSI_FILE).asInstanceOf[ScalaFile]
    if (file == null) return

    // https://youtrack.jetbrains.com/issue/SCL-25618#focus=Comments-27-14327746.0-0
    org.jetbrains.plugins.scala.semantic.DesugarCodeAction.actionPerformed(event)
    return

    val editor = event.getDataContext.getData(CommonDataKeys.EDITOR)
    if (editor == null) return

    val selection = editor.getSelectionModel

    val param = if (selection.hasSelection) ScalaBundle.message("scope.selection") else ScalaBundle.message("scope.file", file)
    val title = ScalaBundle.message("desugar.scala.code.in.scope", param)

    new SelectionDialog().show(title).filter(_.nonEmpty).foreach { transformers =>
      inWriteCommandAction {
          val range = selection.hasSelection.option {
            val document = editor.getDocument

            val marker = document.createRangeMarker(selection.getSelectionStart, selection.getSelectionEnd)
            marker.setGreedyToLeft(true)
            marker.setGreedyToRight(true)

            marker
          }

          withProgressSynchronously(title) {
            try {
              Transformer.applyTransformersAndReformat(file, range, transformers)
            } finally {
              range.foreach(_.dispose())
            }
          }
        }
    }
  }

  override def update(e: AnActionEvent): Unit =
    ScalaActionUtil.enableAndShowIfInScalaFile(e)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
