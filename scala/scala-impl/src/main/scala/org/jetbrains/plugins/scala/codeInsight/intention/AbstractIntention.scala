package org.jetbrains.plugins.scala.codeInsight.intention

import consulo.localize.LocalizeValue

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.Nls

abstract class AbstractIntention(@Nls text: String, @Nls familyName: String)
                                (f: (Project, Editor) => PartialFunction[PsiElement, Unit])
  extends PsiElementBaseIntentionAction {

  override def getText: LocalizeValue = LocalizeValue.of(text)

  def getFamilyName: String = familyName

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    f(project, editor).isDefinedAt(element)

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    f.apply(project, editor)(element)
}
