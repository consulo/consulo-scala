package org.jetbrains.plugins.scala.lang.refactoring.ui

import consulo.language.editor.refactoring.ui.ComboBoxVisibilityPanel
import org.jetbrains.plugins.scala.lang.refactoring.ui.ScalaComboBoxVisibilityPanel._

class ScalaComboBoxVisibilityPanel(additional: String*)
        extends ComboBoxVisibilityPanel[String](options(additional), names(additional))

object ScalaComboBoxVisibilityPanel {
  private def modifiers(additional: Seq[String]) =
    (Seq("private[this]", "private", "protected[this]", "protected") ++ additional.diff(Seq("", "public"))).sorted.distinct

  def options(additional: Seq[String]): Array[String] = (modifiers(additional) :+ "").toArray
  def names(additional: Seq[String]): Array[String] = (modifiers(additional) :+ "public").toArray
}
