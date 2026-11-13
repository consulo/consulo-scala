package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.language.editor.inspection.ProblemsHolder
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}

import scala.collection.immutable.ArraySeq

class SideEffectsInMonadicTransformationInspection extends OperationOnCollectionInspection {

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case _ `.monadicMethod`(arg) =>
      exprsWithSideEffects(arg).foreach(
        expr => holder.registerProblem(expr, ScalaInspectionBundle.message("displayname.side.effects.in.a.monadic.transformation"))
      )
    case _ =>
  }

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq.empty
}
