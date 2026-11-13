package org.jetbrains.plugins.scala.annotator.hints

import consulo.language.editor.intention.IntentionAction
import consulo.language.editor.ui.awt.HintUtil
import consulo.codeEditor.Editor
import consulo.ui.ex.popup.{JBPopup, JBPopupFactory}
import consulo.ui.ex.popup.event.{JBPopupListener, LightweightWindowEvent}
import consulo.util.lang.ref.Ref
import consulo.language.psi.PsiElement
import consulo.util.lang.StringUtil
import consulo.application.util.concurrent.AppExecutorUtil
import org.jetbrains.plugins.scala.autoImport.quickFix.PopupPosition

import java.awt.Point
import java.awt.event.{KeyEvent, KeyListener}
import javax.swing.event.{HyperlinkEvent, HyperlinkListener}
import java.util.concurrent.TimeUnit

private class PopupUI(override val message: String,
                      val popup: JBPopup)
  extends TooltipUI {

  override protected def showImpl(editor: Editor, mousePoint: Point, inlayOffset: Int): Unit = {
    val underLineY = editor.offsetToXY(inlayOffset).y + editor.getLineHeight
    val popupWidth = popup.getContent.getPreferredSize.width

    val point = new Point(
      mousePoint.x + editor.getContentComponent.getLocationOnScreen.x - popupWidth / 8,
      underLineY + editor.getContentComponent.getLocationOnScreen.y
    )

    PopupPosition.at(point).showPopup(popup, editor)
    popup.setUiVisible(false)

    val makeVisible: Runnable = () => {
      if (!popup.isDisposed)
        popup.setUiVisible(true)
    }

    AppExecutorUtil.getAppScheduledExecutorService.schedule(makeVisible, 300L, TimeUnit.MILLISECONDS)
  }

  override def cancel(): Unit = popup.cancel()

  override def addHideListener(action: () => Unit): Unit = popup.addListener(new JBPopupListener {
    override def onClosed(event: LightweightWindowEvent): Unit = action()
  })

  override def isDisposed: Boolean = popup.isDisposed
}

private object PopupUI {
  private val ActionHref = "action"

  // upstream: the content was rendered by LineTooltipRenderer with a TooltipAction (internal API in Consulo),
  // here it is an information label with a link to the action ("show all actions" link is not available)
  def apply(message: String, action: IntentionAction, element: PsiElement, editor: Editor): PopupUI = {
    val uiRef = Ref.create[PopupUI]()

    val text = s"<html><body>$message<br/><a href=\"$ActionHref\">${StringUtil.escapeXmlEntities(action.getText.get)}</a></body></html>"
    val hyperlinkListener: HyperlinkListener = event =>
      if (event.getEventType == HyperlinkEvent.EventType.ACTIVATED && event.getDescription == ActionHref)
        execute(action, element, editor, uiRef.get())

    val component = HintUtil.createInformationLabel(text, hyperlinkListener, null, null)
    component.setOpaque(true)

    val popup = JBPopupFactory.getInstance.createComponentPopupBuilder(component, null)
      .setCancelOnClickOutside(true)
      .setCancelOnOtherWindowOpen(true)
      .setCancelOnWindowDeactivation(true)
      .setCancelOnMouseOutCallback(event => component.contains(event.getPoint))
      .setModalContext(false)
      .createPopup

    val keyListener: KeyListener = new KeyListener {
      private def cancel(): Unit = {
        popup.cancel()
        editor.getContentComponent.removeKeyListener(this)
      }
      override def keyTyped(e: KeyEvent): Unit = cancel()
      override def keyPressed(e: KeyEvent): Unit = ()
      override def keyReleased(e: KeyEvent): Unit = ()
    }
    editor.getContentComponent.addKeyListener(keyListener)

    val ui = new PopupUI(message, popup)
    uiRef.set(ui)
    ui
  }

  private def execute(action: IntentionAction, element: PsiElement, editor: Editor, popupUi: PopupUI): Unit = {
    val popupLocation = popupUi.popup.getLocationOnScreen
    popupUi.cancel()
    if (element.isValid) {
      PopupPosition.withCustomPopupLocation(editor, popupLocation) {
        action.invoke(element.getProject, editor, element.getContainingFile)
      }
    }
  }
}
