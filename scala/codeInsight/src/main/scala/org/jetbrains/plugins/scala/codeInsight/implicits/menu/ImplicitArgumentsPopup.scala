package org.jetbrains.plugins.scala.codeInsight.implicits.menu

import consulo.annotation.component.ActionImpl

import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.actions.implicitArguments.ShowImplicitArgumentsAction
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInsight.implicits.{ImplicitHint, MouseHandler}
import org.jetbrains.plugins.scala.lang.psi.api.ImplicitArgumentsOwner

@ActionImpl(id = "ExplicitArgumentMenu.ShowImplicitArgumentsPopup")
class ImplicitArgumentsPopup extends AnAction(
  LocalizeValue.of(ScalaCodeInsightBundle.message("implicit.arguments.popup.action.text")),
  LocalizeValue.of(ScalaCodeInsightBundle.message("implicit.arguments.popup.action.description")),
  null
) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor == null) return
    val model = editor.getInlayModel

    val inlay = model.getElementAt(MouseHandler.mousePressLocation)
    if (inlay == null) return

    ImplicitHint.elementOf(inlay) match {
      case ImplicitArgumentsOwner(argsByClause) =>
        //@TODO: adapt to multiple/interleaved implicit clauses
        ShowImplicitArgumentsAction.showPopup(editor, argsByClause.flatMap(_.args), isConversion = false)
      case _ =>
    }
  }
}
