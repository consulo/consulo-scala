package org.jetbrains.plugins.scala.actions

import consulo.ui.ex.awt.UIUtil
import consulo.language.editor.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import consulo.language.editor.ui.awt.HintUtil
import consulo.ui.ex.action.AnActionEvent
import consulo.language.editor.CommonDataKeys
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile
import consulo.ui.ex.awt.hint.LightweightHint
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.awt.Point
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

    val hint: LightweightHint = new LightweightHint(label)

    val hintManager: HintManagerImpl = HintManagerImpl.getInstanceImpl

    label.addMouseMotionListener(new MouseMotionAdapter {
      override def mouseMoved(e: MouseEvent): Unit = {
        hintManager.hideAllHints()
      }
    })

    val position = editor.getCaretModel.getLogicalPosition
    val p: Point = HintManagerImpl.getHintPosition(hint, editor, position, HintManager.ABOVE)

    hintManager.showEditorHint(hint, editor, p,
      HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false)
  }
}
