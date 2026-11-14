package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.ui.ex.action.{AnActionEvent, ToggleAction}
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

@ActionImpl(id = "Scala.ShowImplicits", parents = Array(new ActionParentRef(value = new ActionRef(id = "CodeEditorViewGroup"), anchor = ActionRefAnchor.LAST)))
class ShowImplicitHintsAction extends ToggleAction(
  LocalizeValue.of(ScalaCodeInsightBundle.message("show.implicit.hints.action.text")),
  LocalizeValue.of(ScalaCodeInsightBundle.message("show.implicit.hints.action.description")),
  /* icon = */ null
) {

  override def isSelected(event: AnActionEvent): Boolean = ImplicitHints.enabled

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    ImplicitHints.enabled = state
    ImplicitHints.updateInAllEditors()
    if (!state) {
      MouseHandler.removeEscKeyListeners()
    }
  }
}

private object ShowImplicitHintsAction {
  val Id = "Scala.ShowImplicits"
}
