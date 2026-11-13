package org.jetbrains.plugins.scala.lang.refactoring.ui

import com.intellij.java.impl.codeInsight.daemon.impl.JavaReferenceImporter
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.editor.refactoring.ui.CodeFragmentTableCellEditorBase
import org.jetbrains.plugins.scala.api.ScalaFileType

class ScalaCodeFragmentTableCellEditor(project: Project)
  extends CodeFragmentTableCellEditorBase(project, ScalaFileType.INSTANCE) {

  override def stopCellEditing: Boolean = {
    val editor: Editor = myEditorTextField.getEditor
    if (editor != null) {
      val offset = editor.getCaretModel.getOffset
      new JavaReferenceImporter().autoImportReferenceAt(editor, myCodeFragment, offset)
    }
    super.stopCellEditing
  }

}
