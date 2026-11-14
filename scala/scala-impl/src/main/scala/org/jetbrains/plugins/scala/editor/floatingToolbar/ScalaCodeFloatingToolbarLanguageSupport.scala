package org.jetbrains.plugins.scala.editor.floatingToolbar

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.floatingToolbar.CodeFloatingToolbarLanguageSupport
import org.jetbrains.plugins.scala.api.ScalaLanguage

@ExtensionImpl
final class ScalaCodeFloatingToolbarLanguageSupport extends CodeFloatingToolbarLanguageSupport {
  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
