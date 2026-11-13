package org.jetbrains.plugins.scala.annotator.quickfix

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScFor, ScGenerator}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

final class AddCaseToGeneratorQuickfix(gen: ScGenerator) extends PsiUpdateFix[ScGenerator](gen) with DumbAware {
  override def getFamilyName: String = ScalaInspectionBundle.message("add.case")

  override def invoke(context: PsiUpdateContext, gen: ScGenerator): Unit = {
    if (gen.caseKeyword.isDefined) ()
    else {
        gen.replace(
          ScalaPsiElementFactory.createExpressionFromText(s"for { case ${gen.getText} } ()", gen)(using gen)
            .asInstanceOf[ScFor].enumerators.head.generators.head
        )
        ()
      }
  }
}