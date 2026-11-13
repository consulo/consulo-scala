package org.jetbrains.plugins.scala.codeInspection.quickfix

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.quickfix.WrapInBackticksQuickFix.message
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScReference, ScStableCodeReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ProjectContext

abstract class WrapInBackticksQuickFix[T <: ScReference](reference: T, referenceBuilder: (String, ProjectContext) => T)
  extends PsiUpdateFix[T](reference) {
  override def getFamilyName: String = message

  override def invoke(context: PsiUpdateContext, reference: T): Unit =
    reference.replace(referenceBuilder(s"`${reference.refName}`", context.project))
}

object WrapInBackticksQuickFix {
  val message: String = ScalaInspectionBundle.message("wrap.in.backticks")
}

final class WrapRefExprInBackticksQuickFix(ref: ScReferenceExpression)
  extends WrapInBackticksQuickFix(ref, ScalaPsiElementFactory.createReferenceExpressionFromText(_)(using _))

final class WrapStableCodeRefInBackticksQuickFix(ref: ScStableCodeReference)
  extends WrapInBackticksQuickFix(ref, ScalaPsiElementFactory.createReferenceFromText(_)(using _))
