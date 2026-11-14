package org.jetbrains.plugins.scala.codeInsight.implicits.menu

import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.disposer.Disposer
import org.jetbrains.plugins.scala.codeInsight.implicits.{ImplicitHint, MouseHandler, TextPartsHintRenderer}
import org.jetbrains.plugins.scala.extensions.inWriteCommandAction
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaCode.*
import org.jetbrains.plugins.scala.project.ProjectContext

class MakeArgumentsExplicit extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val model = editor.getInlayModel

    val inlay = model.getElementAt(MouseHandler.mousePressLocation)
    val element = ImplicitHint.elementOf(inlay)

    val inlayText = inlay.getRenderer.asInstanceOf[TextPartsHintRenderer].getText

    implicit val context: ProjectContext = ProjectContext.fromProject(e.getData(CommonDataKeys.PROJECT))

    inWriteCommandAction(element.replace(code"$element$inlayText"))(using editor.getProject)

    Disposer.dispose(inlay)
  }
}
