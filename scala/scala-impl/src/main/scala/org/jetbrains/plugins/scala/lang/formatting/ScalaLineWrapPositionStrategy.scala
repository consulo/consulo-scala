package org.jetbrains.plugins.scala.lang.formatting

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.codeEditor.DefaultLineWrapPositionStrategy
import consulo.document.Document

@ExtensionImpl
class ScalaLineWrapPositionStrategy extends DefaultLineWrapPositionStrategy {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def canUseOffset(document: Document, offset: Int, virtual: Boolean): Boolean = {
    val chars = document.getCharsSequence
    val charAtOffset = chars.charAt(offset)

    if (charAtOffset == '.') {
      if (offset > 0 && Character.isDigit(chars.charAt(offset - 1))) return false
      if (offset + 1 < chars.length && Character.isDigit(chars.charAt(offset + 1))) return false
    }

    if (charAtOffset == ' ' && offset > 0 && chars.charAt(offset - 1) == ':') return false
    if (charAtOffset == ':' && offset + 1 < chars.length() && chars.charAt(offset - 1) == ' ') return false
    true
  }
}
