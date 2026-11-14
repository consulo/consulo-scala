package org.jetbrains.plugins.scala.debugger

import com.intellij.debugger.SourcePosition
import consulo.codeEditor.Editor
import consulo.language.psi.{PsiElement, PsiFile}

private abstract class AbstractScalaSourcePosition(delegate: SourcePosition) extends SourcePosition {
  override def getFile: PsiFile = delegate.getFile
  override def getElementAt: PsiElement = delegate.getElementAt
  override def getLine: Int = delegate.getLine
  override def getOffset: Int = delegate.getOffset
  override def openEditor(requestFocus: Boolean): Editor = delegate.openEditor(requestFocus)
}
