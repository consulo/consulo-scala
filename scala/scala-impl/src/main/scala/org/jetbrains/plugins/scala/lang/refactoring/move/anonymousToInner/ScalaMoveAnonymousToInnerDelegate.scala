package org.jetbrains.plugins.scala.lang.refactoring.move.anonymousToInner

import consulo.annotation.component.ExtensionImpl

import consulo.dataContext.DataContext
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.editor.refactoring.move.MoveHandlerDelegate
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition

/**
 * @note original implementation was inspired by [[com.intellij.refactoring.anonymousToInner.MoveAnonymousToInnerHandler]]
 */
@ExtensionImpl(id = "scalaMoveAnonymousToInnerDelegate", order = "before MoveAnonymousToInnerHandler, before MoveInnerToUpperOrMembersHandler, before MoveInnerToUpperHandler, before MoveMembersHandler, before MoveInstanceMethodHandlerDelegate, before JavaMoveClassesOrPackagesHandler, before moveJavaFileOrDir, before moveFileOrDir")
class ScalaMoveAnonymousToInnerDelegate extends MoveHandlerDelegate {

  // upstream: getActionName override "move.anonymousToInner.name" (no such method in Consulo MoveHandlerDelegate)

  override def canMove(elements: Array[PsiElement], targetContainer: PsiElement): Boolean =
    canMove(elements, reference = null)

  private def canMove(elements: Array[PsiElement], reference: PsiReference): Boolean =
    elements.nonEmpty && // in some strange edge cases the array might be empty
      findNewTemplateDefinition(elements(0), reference).exists(_.isAnonymous)

  private def findNewTemplateDefinition(element: PsiElement, reference: PsiReference): Option[ScNewTemplateDefinition] = {
    val maybeElement = Option(reference).map(_.getElement).orElse(Option(element))
    maybeElement.flatMap(element => Option(PsiTreeUtil.getParentOfType(element, classOf[ScNewTemplateDefinition])))
  }

  override def tryToMove(element: PsiElement, project: Project, dataContext: DataContext, reference: PsiReference, editor: Editor): Boolean = {
      val canRefactor = canMove(Array(element), reference = reference)
      if (canRefactor) {
        val maybeNewTemplateDefinition = findNewTemplateDefinition(element, reference)
        maybeNewTemplateDefinition.foreach(element => ScalaAnonymousToInnerHandler.invoke(project, editor, element))
        maybeNewTemplateDefinition.isDefined
      }
      else false
    }
}
