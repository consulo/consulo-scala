package org.jetbrains.plugins.scala.codeInspection.catchAll

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.types.AddOnlyStrategy
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScCaseClause, ScPattern, ScReferencePattern, ScWildcardPattern}

final class ReplaceDangerousCatchAllQuickFix(caseClause: ScCaseClause)
  extends PsiUpdateFix[ScCaseClause](caseClause) {
  override def getFamilyName: String = ScalaBundle.message("specify.type.of.exception")

  override def invoke(context: PsiUpdateContext, cc: ScCaseClause): Unit = cc.pattern match {
    case None => ()
    case Some(pattern) =>
      {
        val strategy = new AddOnlyStrategy
        pattern match {
          case p: ScWildcardPattern => strategy.patternWithoutType(p)
          case p: ScReferencePattern => strategy.patternWithoutType(p)
          //if the pattern has another type, it's a bug
        }
        ()
      }
  }
}
