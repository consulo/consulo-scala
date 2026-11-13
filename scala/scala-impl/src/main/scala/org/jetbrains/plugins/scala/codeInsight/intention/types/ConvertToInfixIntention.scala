package org.jetbrains.plugins.scala.codeInsight.intention.types

import consulo.language.editor.util.LanguageUndoUtil
import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScParameterizedTypeElement, ScParenthesisedTypeElement, ScTypeArgs}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createTypeElementFromText

/** Converts type element `@@[A, B]` to `(A @@ B)` */
class ConvertToInfixIntention extends PsiElementBaseIntentionAction with DumbAware {
  override def getFamilyName: String = ScalaBundle.message("family.name.use.infix.type.syntax")

  override def getText: String = getFamilyName

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    element match {
      case Parent((ref: ScStableCodeReference) & Parent(Parent(param: ScParameterizedTypeElement)))
       if param.typeArgList.typeArguments.size == 2 && !ref.refName.forall(_.isLetterOrDigit)  => true
      case _ => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    if (element == null || !element.isValid) return
    val paramTypeElement: ScParameterizedTypeElement = PsiTreeUtil.getParentOfType(element, classOf[ScParameterizedTypeElement], false)
    val Seq(targ1, targ2) = paramTypeElement.typeArgList.typeArguments
    val needParens = paramTypeElement.getParent match {
      case _: ScTypeArgs | _: ScParenthesisedTypeElement => false
      case _ => true
    }
    val newTypeText = Seq(targ1, paramTypeElement.typeElement, targ2).map(_.getText).mkString(" ").parenthesize(needParens)
    val newTypeElement = createTypeElementFromText(newTypeText, element)(using element)
    if (paramTypeElement.isValid) {
      val replaced = try {
        paramTypeElement.replace(newTypeElement)
      } catch {
        case npe: NullPointerException =>
          throw new RuntimeException("Unable to replace: %s with %s".format(paramTypeElement, newTypeText), npe)
      }
      LanguageUndoUtil.markPsiFileForUndo(replaced.getContainingFile)
    }
  }
}
