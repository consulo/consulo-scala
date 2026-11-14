package org.jetbrains.plugins.scala.debugger

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.engine.SourcePositionHighlighter
import consulo.document.util.TextRange
import org.jetbrains.plugins.scala.api.ScalaLanguage

@ExtensionImpl
class ScalaSourcePositionHighlighter extends SourcePositionHighlighter {
  override def getHighlightRange(sourcePosition: SourcePosition): TextRange = sourcePosition match {
    case _ if !isScalaLanguage(sourcePosition) => null
    case _: ScalaSourcePositionWithWholeLineHighlighted => null
    case l: ScalaLambdaSourcePosition => l.getElementAt.getTextRange
    case _ => null
  }

  private def isScalaLanguage(sourcePosition: SourcePosition): Boolean =
    sourcePosition.getFile.getLanguage.isKindOf(ScalaLanguage.INSTANCE)
}
