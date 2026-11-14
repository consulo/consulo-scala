package org.jetbrains.plugins.scala
package codeInsight
package intention
package argument

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions.{IteratorExt, ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScBlock, ScExpression}

import scala.annotation.tailrec

/**
  * Jason Zaugg
  */
@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.PermuteArgumentsIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Argument conversion"))
final class PermuteArgumentsIntention extends PsiElementBaseIntentionAction {

  import PermuteArgumentsIntention._

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    check(project, editor, element).isDefined

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    if (!element.isValid) return
    check(project, editor, element).foreach(_.apply())
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.permute.arguments")

  override def getText: String = ScalaCodeInsightBundle.message("permute.arguments.to.match.the.parameter.declaration.order")
}

object PermuteArgumentsIntention {

  private def check(project: Project, editor: Editor, element: PsiElement): Option[() => Unit] = {
    val argList = element.withParentsInFile.takeWhile(!_.is[ScBlock]).findByType[ScArgumentExprList].orNull

    if (argList == null)
      return None

    val argExprs = argList.exprs

    val newArgsExprs = argList.matchedParameters.sortBy {
      case (expr, p) => (p.index, expr.getTextRange.getStartOffset)
    }.flatMap {
      case (expr, p) => argOrNamedArg(expr)
    }

    if (newArgsExprs.size != argExprs.size || newArgsExprs == argExprs)
      return None

    Some(() => argExprs.zip(newArgsExprs).foreach {
      case (arg, newArg) => arg.replace(newArg.copy)
    })
  }

  @tailrec
  private def argOrNamedArg(expr: ScExpression): Option[ScExpression] = expr.getContext match {
    case argList: ScArgumentExprList => Some(expr)
    case context: ScExpression => argOrNamedArg(context)
    case _ => None
  }
}
