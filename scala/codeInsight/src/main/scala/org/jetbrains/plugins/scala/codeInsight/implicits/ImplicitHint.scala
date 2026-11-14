package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.codeEditor.Editor
import consulo.codeEditor.InlayModel
import consulo.util.dataholder.Key
import consulo.language.psi.PsiElement
import consulo.ui.ex.awt.JBUI
import org.jetbrains.plugins.scala.annotator.hints.Hint

import java.awt.Insets
import scala.jdk.CollectionConverters.ListHasAsScala

object ImplicitHint {
  private val EmptyInsets = JBUI.emptyInsets()

  private val ElementKey: Key[PsiElement] = Key.create("SCALA_IMPLICIT_HINT_ELEMENT")

  def elementOf(inlay: Inlay): PsiElement = ElementKey.get(inlay)

  def isImplicitHint(inlay: Inlay): Boolean = inlay.getUserData(ElementKey) != null

  def addTo(hint: Hint, model: InlayModel): Option[Inlay] = {
    import hint._

    val offset = position.getOffset(hint.element) + offsetDelta

    val existingInlays = model.getInlineElementsInRange(offset, offset).asScala.filter(isImplicitHint)
    // Larger priorities appear further left; preserve the order in which hints are collected.
    val priority = -existingInlays.size

    val inlay: consulo.codeEditor.Inlay[TextPartsHintRenderer] = {
      val renderer = new TextPartsHintRenderer(parts, menu, hint.corners) {
        override protected def getMargin(editor: Editor): Insets = margin.getOrElse(EmptyInsets)
      }
      if (ImplicitHints.expanded) {
        renderer.expand()
      }
      model.addInlineElement(offset, relatesToPrecedingElement, priority, renderer)
    }
    // Some editors might not support inlay hints
    if (inlay == null)
      return None

    inlay.putUserData(ElementKey, element)
    Some(inlay)
  }
}
