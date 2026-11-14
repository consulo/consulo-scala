package org.jetbrains.plugins.scala.codeInsight.implicits.menu

import consulo.annotation.component.ActionImpl

import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.localize.LocalizeValue
import consulo.disposer.Disposer
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInsight.implicits.{ImplicitHint, MouseHandler, TextPartsHintRenderer}
import org.jetbrains.plugins.scala.codeInsight.intention.expression.MakeImplicitConversionExplicit

import scala.jdk.CollectionConverters.*

@ActionImpl(id = "MakeExplicit")
class MakeConversionExplicit extends AnAction(
  LocalizeValue.of(ScalaCodeInsightBundle.message("make.conversion.explicit.action.text")),
  LocalizeValue.of(ScalaCodeInsightBundle.message("make.conversion.explicit.action.description")),
  null
) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val model = editor.getInlayModel

    val inlay = model.getElementAt(MouseHandler.mousePressLocation)
    val element = ImplicitHint.elementOf(inlay)

    new MakeImplicitConversionExplicit().invoke(e.getData(CommonDataKeys.PROJECT), editor, element)

    val prefixAndSuffixInlays =
      if (inlay == null) Seq()
      else {
        val startOffset = inlay.getOffset
        val endOffset = {
          val inlayText = inlay.getRenderer.asInstanceOf[TextPartsHintRenderer].getText
          startOffset + inlayText.length + element.getTextLength + 1
        }

        model.getInlineElementsInRange(startOffset, endOffset).asScala
      }

    prefixAndSuffixInlays.foreach(Disposer.dispose)
  }
}
