package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import consulo.codeEditor.Editor
import consulo.content.scope.SearchScope
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter

import java.util

@ExtensionImpl(order = "first")
class RenameScalaSyntheticParamProcessor extends RenamePsiElementProcessor with ScalaRenameProcessor {
  override def canProcessElement(element: PsiElement): Boolean = realParamForSyntheticParam(element).isDefined

  override def findReferences(element: PsiElement,
                              searchInCommentsAndStrings: Boolean): util.Collection[PsiReference] =
    super[RenamePsiElementProcessor].findReferences(element, searchInCommentsAndStrings)

  override def substituteElementToRename(element: PsiElement, editor: Editor): PsiElement = realParamForSyntheticParam(element).orNull

  private def realParamForSyntheticParam(element: PsiElement) = element match {
    case x: ScParameter => ScalaPsiUtil.parameterForSyntheticParameter(x)
    case _ => None
  }
}