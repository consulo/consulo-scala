package org.jetbrains.plugins.scala.lang.psi.tree

import consulo.language.Language
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.api.ScalaLanguage

class IScalaElementType(debugName: String,
                        language: Language = ScalaLanguage.INSTANCE,
                        override val isLeftBound: Boolean = true) extends IElementType(debugName, language) {
  override final def toString: String = super.toString
}
