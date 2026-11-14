package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.codeEditor.Editor
import consulo.util.dataholder.Key
import consulo.language.psi.PsiFile

private class ModificationCount(key: String) {
  private val CountKey = Key.create[Long](key)

  def apply(file: PsiFile): Long = file.getManager.getModificationTracker.getModificationCount

  def apply(editor: Editor): Long = CountKey.get(editor, 0L)

  def update(editor: Editor, count: Long): Unit = {
    editor.putUserData(CountKey, count)
  }
}
