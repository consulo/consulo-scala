package org.jetbrains.plugins.scala
package codeInsight
package intention
package argument

import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.codeStyle.CodeStyleManager
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScBlockExpr, ScExpression, ScFunctionExpr, ScUnderscoreSection}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.{createAnonFunBlockFromFunExpr, createBlockFromExpr}
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ArgumentToBlockExpressionIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Argument conversion"))
final class ArgumentToBlockExpressionIntention extends PsiElementBaseIntentionAction with DumbAware {

  import ArgumentToBlockExpressionIntention.{FunctionExpression, argListForElement}

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    elementAndTouchingPrevElement(editor, element).exists { element =>
      IntentionAvailabilityChecker.checkIntention(this, element) &&
        argListForElement(element).exists(list => list.exprs.sizeIs == 1 && !list.exprs.head.is[ScUnderscoreSection])
    }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    elementAndTouchingPrevElement(editor, element)
      .iterator
      .flatMap(argListForElement)
      .nextOption()
      .foreach { list =>
        import list.projectContext
        val exp = list.exprs.head
        val block = exp match {
          case FunctionExpression(fnExpr) => createAnonFunBlockFromFunExpr(fnExpr, element)
          case block: ScBlockExpr => ScalaPsiUtil.convertBlockToBraced(block)
          case _ => createBlockFromExpr(exp, element)
        }
        if (list.isArgsInParens) {
          list.getLastChild.delete()
          list.getFirstChild.replace(block)
          exp.delete()
        } else exp.replace(block)
        CodeStyleManager.getInstance(project).reformat(list)
      }

  def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.convert.to.block.expression")

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)
}

object ArgumentToBlockExpressionIntention {
  private def argListForElement(element: PsiElement): Option[ScArgumentExprList] = element match {
    case ElementType(ScalaTokenTypes.tCOLON) & ChildOf((block: ScBlockExpr) & ChildOf(argList: ScArgumentExprList)) =>
      Option.when(block.firstChildNotWhitespaceComment.contains(element))(argList)
    case ChildOf(list: ScArgumentExprList) => Some(list)
    case _ => None
  }

  private object FunctionExpression {
    def unapply(expr: ScExpression): Option[ScFunctionExpr] = expr match {
      case fnExpr: ScFunctionExpr => Some(fnExpr)
      case block: ScBlockExpr if block.exprs.sizeIs == 1 =>
        block.exprs.headOption.filterByType[ScFunctionExpr]
      case _ => None
    }
  }
}
