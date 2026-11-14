package org.jetbrains.plugins.scala.codeInsight.implicits

import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.awt.{Font, FontMetrics, RenderingHints}
import consulo.application.util.registry.Registry
import consulo.codeEditor.{Editor, RealEditor}
import consulo.ui.ex.awt.UIUtil
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil

private class MyFontMetrics(editor: Editor, familyName: String, size: Int) {
  private val font = UIUtil.getFontWithFallback(familyName, Font.PLAIN, size)

  private val (metrics, lineHeight) = {
    val context = getCurrentContext(editor)

    (MyFontMetrics.fontMetricsOf(font, context),
      // We assume this will be a better approximation to a real line height for a given font
      Math.ceil(font.createGlyphVector(context, "Ap").getVisualBounds.getHeight).toInt)
  }

  def getFont: Font = metrics.getFont

  def getMetrics: FontMetrics = metrics

  def getLineHeight: Int = lineHeight

  def isActual(editor: Editor, familyName: String, size: Int): Boolean = {
    val font = metrics.getFont
    if (familyName != font.getFamily || size != font.getSize) {
      return false
    }
    val currentContext = getCurrentContext(editor)
    currentContext.equals(metrics.getFontRenderContext)
  }

  private def getCurrentContext(editor: Editor): FontRenderContext = {
    val editorContext = editor.getContentComponent.getFontMetrics(font).getFontRenderContext

    new FontRenderContext(editorContext.getTransform,
      DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false),
      editor match {
        case _: RealEditor if Registry.is("editor.text.fractional.metrics") => RenderingHints.VALUE_FRACTIONALMETRICS_ON
        case _ => RenderingHints.VALUE_FRACTIONALMETRICS_OFF
      })
  }
}

private object MyFontMetrics {
  // upstream: FontInfo.getFontMetrics(font, context)
  private def fontMetricsOf(font: Font, context: FontRenderContext): FontMetrics = {
    val graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
    try {
      graphics.setTransform(context.getTransform)
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, context.getAntiAliasingHint)
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, context.getFractionalMetricsHint)
      graphics.getFontMetrics(font)
    } finally {
      graphics.dispose()
    }
  }
}
