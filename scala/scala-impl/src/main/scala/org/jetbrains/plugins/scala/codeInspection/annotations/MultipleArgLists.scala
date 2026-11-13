package org.jetbrains.plugins.scala.codeInspection.annotations

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotation

final class MultipleArgLists extends LocalInspectionTool with DumbAware {
  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case annotation: ScAnnotation if annotation.constructorInvocation.arguments.lengthIs > 1 =>
      holder.registerProblem(annotation, ScalaInspectionBundle.message("implementation.limitation.multiple.argument.lists"))
    case _ =>
  }
}
