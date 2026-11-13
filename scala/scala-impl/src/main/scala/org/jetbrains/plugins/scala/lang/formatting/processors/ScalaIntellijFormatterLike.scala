package org.jetbrains.plugins.scala.lang.formatting.processors

import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

trait ScalaIntellijFormatterLike {

  def needToProcess(element: PsiElement, range: TextRange, scalaSettings: ScalaCodeStyleSettings): Boolean  = {
    element match {
      case file: ScalaFile if file.getTextRange == range =>
        scalaSettings.USE_INTELLIJ_FORMATTER()
      case _ =>
        scalaSettings.USE_INTELLIJ_FORMATTER() || scalaSettings.SCALAFMT_USE_INTELLIJ_FORMATTER_FOR_RANGE_FORMAT
    }
  }

}
