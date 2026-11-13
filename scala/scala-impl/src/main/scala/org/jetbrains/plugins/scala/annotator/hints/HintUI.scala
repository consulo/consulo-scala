package org.jetbrains.plugins.scala.annotator.hints

import consulo.language.editor.hint.HintManager
import consulo.language.editor.ui.awt.HintUtil
import consulo.codeEditor.Editor
import consulo.ui.ex.RelativePoint
import consulo.ui.ex.awt.JBUI
import consulo.ui.ex.awt.util.PopupUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle

import java.awt.Point
import java.awt.event.HierarchyEvent
import javax.swing.{JComponent, JLabel}

// upstream: shown via HintManagerImpl.showEditorHint with a LightweightHint (internal API in Consulo)
private class HintUI(override val message: String,
                     component: JComponent)
  extends TooltipUI {

  private var shown = false

  override protected def showImpl(editor: Editor, mousePoint: Point, inlayOffset: Int): Unit = {
    val lineY = editor.visualPositionToXY(editor.xyToVisualPosition(mousePoint)).y
    val point = new Point(mousePoint.x, lineY - component.getPreferredSize.height)

    HintManager.getInstance.showHint(component, new RelativePoint(editor.getContentComponent, point),
      HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0)
    shown = true
  }

  override def isDisposed: Boolean = shown && !component.isShowing

  override def cancel(): Unit = {
    val popup = PopupUtil.getPopupContainerFor(component)
    if (popup != null) popup.cancel()
  }

  override def addHideListener(action: () => Unit): Unit = component.addHierarchyListener { (e: HierarchyEvent) =>
    if ((e.getChangeFlags & HierarchyEvent.SHOWING_CHANGED) != 0 && !component.isShowing) action()
  }
}

private object HintUI {
  def apply(@Nls message: String): TooltipUI = {
    // TODO Why HTML is rewritten by com.intellij.ide.IdeTooltipManager.initPane(com.intellij.util.ui.Html, com.intellij.ui.HintHint, javax.swing.JLayeredPane) ?
    val label = if (message.contains(ScalaBundle.message("type.mismatch.dot"))) new JLabel(message) else HintUtil.createInformationLabel(message)
    label.setBorder(JBUI.Borders.empty(6, 6, 5, 6))

    new HintUI(message, label)
  }
}
