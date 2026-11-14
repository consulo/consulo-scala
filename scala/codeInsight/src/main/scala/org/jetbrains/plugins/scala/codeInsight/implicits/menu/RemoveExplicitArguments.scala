package org.jetbrains.plugins.scala.codeInsight.implicits.menu

import consulo.annotation.component.ActionImpl

import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.localize.LocalizeValue
import consulo.disposer.Disposer
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInsight.implicits.{ImplicitHint, MouseHandler}
import org.jetbrains.plugins.scala.extensions.inWriteCommandAction

@ActionImpl(id = "RemoveExplicitArguments")
class RemoveExplicitArguments extends AnAction(
  LocalizeValue.of(ScalaCodeInsightBundle.message("remove.explicit.arguments.action.text")),
  LocalizeValue.of(ScalaCodeInsightBundle.message("remove.explicit.arguments.action.description")),
  null
) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val model = editor.getInlayModel

    val inlay = model.getElementAt(MouseHandler.mousePressLocation)
    val element = ImplicitHint.elementOf(inlay)

    inWriteCommandAction(element.getParent.replace(element.getPrevSibling))(using editor.getProject)
    Disposer.dispose(inlay)
  }
}
