package org.jetbrains.plugins.scala.codeInspection.targetNameAnnotation

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.extensions.PsiElementExt

trait TargetNameInspectionBase extends LocalInspectionTool {
  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) { element =>
    if (element.isInScala3File) {
      findProblemElement.lift(element).foreach { problemElement =>
        //noinspection ReferencePassedToNls
        holder.registerProblem(
          problemElement.element,
          problemElement.maybeDescription.getOrElse(getDisplayName),
          problemElement.maybeQuickFix.toArray*
        )
      }
    }
  }

  protected def findProblemElement: PartialFunction[PsiElement, ProblemElement]
}
