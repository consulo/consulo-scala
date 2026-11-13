package org.jetbrains.plugins.scala
package incremental

import incremental.Updater._

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.disposer.Disposable
import consulo.codeEditor.Editor
import consulo.colorScheme.EditorColorsScheme
import consulo.codeEditor.EditorEx
import consulo.codeEditor.markup.MarkupModelEx
import consulo.codeEditor.markup.RangeHighlighter
import consulo.util.dataholder.Key
import consulo.ui.color.ColorValue
import consulo.document.util.TextRange
import org.jetbrains.plugins.scala.extensions.inReadAction

import javax.swing.Timer
import scala.collection.mutable.ArrayBuffer

private class Updater(editor: Editor) extends Disposable {
  private val updateTimer = {
    val timer = new Timer(UPDATE_DELAY, _ => update())
    timer.setRepeats(false)
    timer
  }

  private var previousVisibleRange: TextRange = scala.compiletime.uninitialized

  def scheduleUpdate(delta: Boolean): Unit = {
    if (!delta) {
      previousVisibleRange = null
    }

    updateTimer.restart()
  }

  private def update(): Unit = {
    VisibleRange.saveIn(editor)

    val visibleRange = VisibleRange.in(editor)

    val exactVisibleRange = VisibleRange.exactIn(editor)
    val editorEx = editor.asInstanceOf[EditorEx]
    Seq(editorEx.getMarkupModel, editorEx.getFilteredDocumentMarkupModel).foreach { model =>
      concealErrorStripeMarksOutside(exactVisibleRange, model, editor.getColorsScheme)
      revealErrorStripeMarksInside(exactVisibleRange, model)
    }

    val newlyVisibleRange = if (previousVisibleRange == null) visibleRange else visibleRange.diff(previousVisibleRange)
    if (!newlyVisibleRange.isEmpty) {
      val document = editor.getDocument
      val daemon = DaemonCodeAnalyzer.getInstance(editor.getProject)
      inReadAction(daemon.combineDirtyScopes(document, newlyVisibleRange))
      daemon.stopProcess(editor.getProject, document)
    }

    previousVisibleRange = visibleRange
  }

  override def dispose(): Unit = {
    updateTimer.stop()
  }
}

private object Updater {
  private val UPDATE_DELAY = 200 // ms

  private val ERROR_STRIPE_MARK_COLOR_KEY = Key.create[ColorValue]("error_stripe_mark_color")

  private def concealErrorStripeMarksOutside(visibleRange: TextRange, markupModel: MarkupModelEx, colorScheme: EditorColorsScheme): Unit = {
    val outsideHighlighters = {
      val buffer = ArrayBuffer.empty[RangeHighlighter]
      markupModel.processRangeHighlightersOutside(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => { buffer += highlighter; true })
      buffer
    }
    outsideHighlighters.foreach { highlighter =>
      concealErrorStripeMark(highlighter, colorScheme)
    }
  }

  def concealErrorStripeMark(highlighter: RangeHighlighter, colorScheme: EditorColorsScheme): Unit = {
    val backgroundColor = colorScheme.getDefaultBackground
    val actualColor = highlighter.getErrorStripeMarkColor(colorScheme)
    if (!highlighter.isThinErrorStripeMark && actualColor != null && actualColor != backgroundColor) {
      highlighter.putUserData(ERROR_STRIPE_MARK_COLOR_KEY, actualColor)
      highlighter.setErrorStripeMarkColor(backgroundColor)
    }
  }

  private def revealErrorStripeMarksInside(visibleRange: TextRange, markupModel: MarkupModelEx): Unit = {
    val insideHighlighters = {
      val buffer = ArrayBuffer.empty[RangeHighlighter]
      markupModel.processRangeHighlightersOverlappingWith(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => { buffer += highlighter; true })
      buffer
    }
    insideHighlighters.foreach { highlighter =>
      val savedColor = highlighter.getUserData(ERROR_STRIPE_MARK_COLOR_KEY)
      if (savedColor != null) {
        highlighter.setErrorStripeMarkColor(savedColor)
        highlighter.putUserData(ERROR_STRIPE_MARK_COLOR_KEY, null)
      }
    }
  }
}
