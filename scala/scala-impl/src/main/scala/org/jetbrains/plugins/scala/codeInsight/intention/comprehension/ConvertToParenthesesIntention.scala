package org.jetbrains.plugins.scala.codeInsight.intention.comprehension

import consulo.localize.LocalizeValue

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.ast.TokenType
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.comprehension.ConvertToParenthesesIntention._
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScFor
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

object ConvertToParenthesesIntention {
  val FamilyName: String = ScalaBundle.message("intention.for.comprehension.convert.to.parentheses")
}

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ConvertToParenthesesIntention", fileExtensions = Array("scala"), categories = Array("Scala", "for comprehension"))
class ConvertToParenthesesIntention extends PsiElementBaseIntentionAction with DumbAware {
  def getFamilyName: String = FamilyName

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    element match {
      case e @ Parent(_: ScFor) =>
        List(ScalaTokenTypes.tLBRACE, ScalaTokenTypes.tRBRACE).contains(e.getNode.getElementType) && 
          IntentionAvailabilityChecker.checkIntention(this, element)
      case _ => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val statement = element.getParent.asInstanceOf[ScFor]
    ScalaPsiUtil.replaceBracesWithParentheses(statement)

    val manager = statement.getManager
    for (enumerators <- statement.enumerators;
         cr <- enumerators.findChildrenByType(TokenType.WHITE_SPACE) if cr.textContains('\n')) {
      cr.replace(ScalaPsiElementFactory.createSemicolon(using manager))
    }

    for (cr <- statement.findChildrenByType(TokenType.WHITE_SPACE) if cr.textContains('\n')) {
      cr.delete()
    }
  }
}
