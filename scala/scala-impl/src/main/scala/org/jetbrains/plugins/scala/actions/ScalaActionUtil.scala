package org.jetbrains.plugins.scala.actions

import consulo.ui.ex.awt.UIUtil
import consulo.language.editor.hint.HintManager
import consulo.language.editor.ui.awt.HintUtil
import consulo.ui.ex.action.AnActionEvent
import consulo.language.editor.CommonDataKeys
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.awt.event.{MouseEvent, MouseMotionAdapter}

object ScalaActionUtil {

  def enableAndShowIfInScalaFile(e: AnActionEvent): Unit = {
    val isInScalaFile = getFileFrom(e).exists(_.is[ScalaFile])
    e.getPresentation.setEnabledAndVisible(isInScalaFile)
  }

  def getFileFrom(e: AnActionEvent): Option[PsiFile] = Option(e.getDataContext.getData(CommonDataKeys.PSI_FILE))

  @RequiresEdt
  def showHint(editor: Editor, @Nls text: String): Unit = {
    val label = HintUtil.createInformationLabel(text)
    label.setFont(UIUtil.getLabelFont)

    val hintManager: HintManager = HintManager.getInstance

    label.addMouseMotionListener(new MouseMotionAdapter {
      override def mouseMoved(e: MouseEvent): Unit = {
        hintManager.hideAllHints()
      }
    })

    // upstream: HintManagerImpl.showEditorHint(hint, editor, position, HIDE_BY_ANY_KEY | HIDE_BY_TEXT_CHANGE | HIDE_BY_SCROLLING, ...)
    hintManager.showInformationHint(editor, label)
  }
}
