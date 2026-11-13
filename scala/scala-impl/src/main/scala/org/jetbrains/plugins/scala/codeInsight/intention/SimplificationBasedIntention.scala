package org.jetbrains.plugins.scala.codeInsight.intention

import consulo.localize.LocalizeValue

import org.jetbrains.plugins.scala.codeInspection.PsiUpdateFix
import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.collections.{Simplification, SimplificationType}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlockExpr, ScExpression}

abstract class SimplificationBasedIntention(
  @Nls familyName: String,
  simplificationType: SimplificationType,
  quickFix: Simplification => PsiUpdateFix[?]
) extends PsiElementBaseIntentionAction {
  def getText: LocalizeValue = LocalizeValue.of(familyName)

  def getFamilyName: String = familyName

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    simplification(element).isDefined

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    simplification(element).foreach { simplification =>
      val intentionAction = quickFix(simplification)
      intentionAction.invoke(project, editor, element.getContainingFile)
    }

  private def simplification(element: PsiElement): Option[Simplification] =
    element.withParentsInFile.takeWhile(!_.is[ScBlockExpr]).flatMap {
      case e: ScExpression => simplificationType.getSimplification(e)
      case _ => None
    }.nextOption()
}
