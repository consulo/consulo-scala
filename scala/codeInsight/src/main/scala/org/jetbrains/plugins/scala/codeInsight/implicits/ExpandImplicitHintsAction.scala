package org.jetbrains.plugins.scala
package codeInsight
package implicits

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction

@ActionImpl(id = "Scala.ExpandImplicits", parents = Array(new ActionParentRef(value = new ActionRef(id = "CodeEditorViewGroup"), anchor = ActionRefAnchor.AFTER, relatedToAction = new ActionRef(id = "Scala.ShowImplicits"))))
class ExpandImplicitHintsAction extends ToggleAction(
  ScalaCodeInsightBundle.message("expand.implicit.hints.action.text"),
  ScalaCodeInsightBundle.message("expand.implicit.hints.action.description"),
  /* icon = */ null
) {
  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setEnabled(ImplicitHints.enabled)
  }

  override def isSelected(e: AnActionEvent): Boolean =
    ImplicitHints.expanded

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    ImplicitHints.expanded = state

    val editor = e.getData(CommonDataKeys.EDITOR)

    if (editor != null) {
      if (state) {
        ImplicitHints.expandIn(editor)
      } else {
        ImplicitHints.collapseIn(editor)
      }
    } else {
      ImplicitHints.updateInAllEditors()
    }

    if (state) {
      MouseHandler.removeEscKeyListeners()
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}

object ExpandImplicitHintsAction {
  val Id = "Scala.ExpandImplicits"
}
