package org.jetbrains.plugins.scala.codeInsight

import consulo.ui.ex.action.{KeyboardShortcut, Shortcut}
import consulo.codeEditor.{EditorCustomElementRenderer, InlayModel}
import consulo.ui.ex.keymap.{Keymap, KeymapManager}
import consulo.util.dataholder.Key
import consulo.document.util.TextRange
import org.jetbrains.plugins.scala.annotator.hints.Hint

import scala.jdk.CollectionConverters._
import scala.collection.mutable

package object implicits {
  private val ScalaImplicitHintKey = Key.create[Boolean]("SCALA_IMPLICIT_HINT")

  private[implicits] type Inlay = consulo.codeEditor.Inlay[? <: EditorCustomElementRenderer]

  implicit class Model(private val model: InlayModel) extends AnyVal {
    def inlaysIn(range: TextRange): collection.Seq[Inlay] =
      model.getInlineElementsInRange(range.getStartOffset, range.getEndOffset, classOf[EditorCustomElementRenderer])
        .asScala
        .filter(ScalaImplicitHintKey.isIn)

    def add(hint: Hint): Unit = {
      val inlay = ImplicitHint.addTo(hint, model)
      inlay.foreach(_.putUserData(ScalaImplicitHintKey, true))
    }
  }

  class ShortcutManager {
    case class ManagedShortcut private[ShortcutManager](shortCut: Shortcut)

    private val allShortCuts = mutable.Buffer.empty[Shortcut]

    protected def register(shortcut: Shortcut): ManagedShortcut = {
      allShortCuts += shortcut
      ManagedShortcut(shortcut)
    }

    def setShortcuts(id: String, shortcuts: Seq[this.ManagedShortcut]): Unit = {
      val keymap = KeymapManager.getInstance().getActiveKeymap
      removeAllShortcuts(id)
      shortcuts.foreach(s => keymap.addShortcut(id, s.shortCut))
    }

    def removeAllShortcuts(id: String): Unit = {
      removeAllShortcuts(KeymapManager.getInstance().getActiveKeymap, id)
    }
    private def removeAllShortcuts(keymap: Keymap, id: String): Unit = {
      allShortCuts.foreach(keymap.removeShortcut(id, _))
    }
  }

  object ImplicitShortcuts extends ShortcutManager {
    val EnableShortcuts = Seq(
      register(KeyboardShortcut.fromString("control alt shift EQUALS")),
      register(KeyboardShortcut.fromString("control alt shift ADD")),
    )

    val DisableShortcuts = Seq(
      register(KeyboardShortcut.fromString("control alt shift MINUS")),
      register(KeyboardShortcut.fromString("control alt shift SUBTRACT")),
    )
  }


  def pairFor[T](element: T, elements: Seq[T], isOpening: T => Boolean, isClosing: T => Boolean): Option[T] = {
    def pairIn(elements: Seq[T]) = {
      var balance = 0
      val remainder = elements.dropWhile(_ != element).dropWhile { it =>
        if (isOpening(it)) balance += 1
        if (isClosing(it)) balance -= 1
        balance != 0
      }
      remainder.headOption
    }

    if (isOpening(element)) pairIn(elements)
    else if (isClosing(element)) pairIn(elements.reverse)
    else None
  }
}
