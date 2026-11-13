package org.jetbrains.plugins.scala.lang.formatting.processors

import consulo.annotation.component.ExtensionImpl

import consulo.document.util.TextRange
import consulo.language.codeStyle.CodeStyleSettings
import consulo.language.codeStyle.PostFormatProcessor
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings

@ExtensionImpl
class ScalaBracePostFormatProcessor extends PostFormatProcessor with ScalaIntellijFormatterLike {
  override def processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    if (needToProcess(source, rangeToReformat, scalaSettings)) {
      new ScalaBraceEnforcer(settings, scalaSettings).processText(source, rangeToReformat)
    } else {
      rangeToReformat
    }
  }

  override def processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    if (needToProcess(source, source.getTextRange, scalaSettings)) {
      new ScalaBraceEnforcer(settings, scalaSettings).process(source)
    } else {
      source
    }
  }
}