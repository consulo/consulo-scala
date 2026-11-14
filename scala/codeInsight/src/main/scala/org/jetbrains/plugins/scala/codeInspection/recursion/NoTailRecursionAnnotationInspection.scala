package org.jetbrains.plugins.scala.codeInspection.recursion

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.annotator.FunctionAnnotator
import org.jetbrains.plugins.scala.codeInsight.intention
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

final class NoTailRecursionAnnotationInspection extends LocalInspectionTool {

  import intention.recursion.AddTailRecursionAnnotationIntention._

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element@CanBeTailRecursive(function)
      if IntentionAvailabilityChecker.checkInspection(this, element) &&
         FunctionAnnotator.canBeTailRecursive(function) =>
      val quickFix = new AbstractFixOnPsiElement(
        ScalaCodeInsightBundle.message("no.tailrec.annotation.fix"),
        function
      ) {
        override protected def doApplyFix(function: ScFunctionDefinition)
                                         (implicit project: Project): Unit = {
          addTailRecursionAnnotation(function)
        }
      }

      holder.registerProblem(element, getDisplayName, quickFix)
    case _ =>
  }
}
