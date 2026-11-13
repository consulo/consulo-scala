package org.jetbrains.plugins.scala.lang.refactoring

import consulo.dataContext.DataContext
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.language.editor.refactoring.action.RefactoringActionHandler

trait ScalaRefactoringActionHandler extends RefactoringActionHandler {

  def invoke(file: PsiFile)
            (implicit project: Project, editor: Editor, dataContext: DataContext): Unit

  def invoke(elements: Array[PsiElement])
            (implicit project: Project, dataContext: DataContext): Unit = {}

  override final def invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext): Unit =
    invoke(file)(using project, editor, dataContext)

  override def invoke(project: Project, elements: Array[PsiElement], dataContext: DataContext): Unit =
    invoke(elements)(using project, dataContext)
}
