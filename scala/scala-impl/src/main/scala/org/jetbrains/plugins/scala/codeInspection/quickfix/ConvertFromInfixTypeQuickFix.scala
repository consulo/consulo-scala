package org.jetbrains.plugins.scala.codeInspection.quickfix

import consulo.language.editor.util.LanguageUndoUtil
import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import consulo.language.editor.util.LanguageUndoUtil.markPsiFileForUndo
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.quickfix.ConvertFromInfixTypeQuickFix.message
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScInfixTypeElement, ScParenthesisedTypeElement}

final class ConvertFromInfixTypeQuickFix(expr: ScInfixTypeElement)
  extends PsiUpdateFix[ScInfixTypeElement](expr)
    with DumbAware {
  override def getFamilyName: String = message

  override def invoke(context: PsiUpdateContext, infixType: ScInfixTypeElement): Unit =
    ConvertFromInfixTypeQuickFix.applyFix(infixType)
}

object ConvertFromInfixTypeQuickFix {
  val message: String = ScalaInspectionBundle.message("convert.from.infix.type")

  def applyFix(infixTypeElement: ScInfixTypeElement): Unit = {
    val replacement = infixTypeElement.computeDesugarizedType
      .getOrElse(return)

    val elementToReplace = infixTypeElement.getParent match {
      case x: ScParenthesisedTypeElement => x
      case _ => infixTypeElement
    }

    elementToReplace.replace(replacement)
    markPsiFileForUndo(replacement.getContainingFile)
  }
}
