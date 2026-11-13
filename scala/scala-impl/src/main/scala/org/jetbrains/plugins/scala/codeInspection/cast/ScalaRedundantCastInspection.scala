package org.jetbrains.plugins.scala.codeInspection.cast

import consulo.localize.LocalizeValue
import consulo.language.editor.impl.inspection.ProblemDescriptorImpl
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnTwoPsiElements, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.{ElementText, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScGenericCall}
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt, TypePresentationContext}

class ScalaRedundantCastInspection extends LocalInspectionTool {

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case call: ScGenericCall =>
      implicit val tpc: TypePresentationContext = TypePresentationContext(call)
      implicit val context: Context = Context(call)

      call.referencedExpr.children.toList match {
        case List(left: ScExpression, ElementText("."), ElementText("asInstanceOf")) =>
          for (actualType <- left.`type`().toOption;
               typeArgument <- call.typeArguments.headOption;
               castType <- typeArgument.`type`().toOption if actualType.widen.equiv(castType)) {

            val descriptor = {
              val range = new TextRange(left.getTextLength, call.getTextLength)

              val message = ScalaInspectionBundle.message("casting.left.to.right.is.redundant", left.getText, castType.presentableText)

              new ProblemDescriptorImpl(call, call, LocalizeValue.of(message), Array(new RemoveCastQuickFix(call, left)), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, range, null, false)
            }

            holder.registerProblem(descriptor)
          }
        case _ =>
      }
    case _ =>
  }

  class RemoveCastQuickFix(c: ScGenericCall, e: ScExpression)
          extends AbstractFixOnTwoPsiElements(ScalaInspectionBundle.message("remove.redundant.cast"), c, e) {


    override protected def doApplyFix(call: ScGenericCall, expr: ScExpression)
                                     (implicit project: Project): Unit = {
      call.getParent.addBefore(expr, call)
      call.delete()
    }
  }
}


