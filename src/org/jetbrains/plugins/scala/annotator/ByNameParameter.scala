package org.jetbrains.plugins.scala
package annotator

import java.awt.Color
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.markup.TextAttributes
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import lang.psi.api.statements.params.ScParameter
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import lang.formatting.settings.ScalaCodeStyleSettings
import com.intellij.openapi.util.TextRange
import lang.psi.api.expr._
import lang.psi.api.base.{ScPrimaryConstructor, ScConstructor, ScLiteral}
import org.jetbrains.plugins.scala.extensions._

/**
 * Pavel Fatin
 */

object ByNameParameter extends AnnotatorPart[ScExpression] {
  private val Foreground = new Color(128, 128, 128)

  def kind = classOf[ScExpression]

  def annotate(exp: ScExpression, holder: AnnotationHolder, typeAware: Boolean) {
    val settings = CodeStyleSettingsManager.getSettings(exp.getProject)
            .getCustomSettings(classOf[ScalaCodeStyleSettings])

    if(!settings.SHOW_ARGUMENTS_TO_BY_NAME_PARAMETERS) return

    if(!settings.INCLUDE_BLOCK_EXPRESSIONS && exp.isInstanceOf[ScBlockExpr]) return

    val parameter = parameterOf(exp).orElse(conversionParameterOf(exp))

    parameter.filter(_.isCallByNameParameter).foreach { p =>
      val attributes = new TextAttributes()
      attributes.setForegroundColor(Foreground)

      val ranges = if(settings.INCLUDE_LITERALS) Seq(exp.getTextRange) else nonLiteralRangesIn(exp)

      ranges.foreach { r =>
        val annotation = holder.createInfoAnnotation(r, null)
        annotation.setEnforcedTextAttributes(attributes)
      }
    }
  }

  private def parameterOf(exp: ScExpression): Option[ScParameter] = {
    exp match {
      case assignment: ScAssignStmt =>
        assignment.getLExpression match {
          case ref: ScReferenceExpression =>
            ref.resolve().asOptionOf(classOf[ScParameter])
          case _ => None
        }
      case _ =>
        exp.getParent match {
          case ie: ScInfixExpr if exp == (if (ie.isLeftAssoc) ie.lOp else ie.rOp) =>
            ie.operation match {
              case Resolved(f: ScFunction, _) => f.parameters.headOption
              case _ => None
            }
          case args: ScArgumentExprList =>
            args.getParent match {
              case constructor: ScConstructor =>
                constructor.reference
                        .flatMap(_.resolve().asOptionOf(classOf[ScPrimaryConstructor]))
                        .flatMap(_.parameters.lift(args.exprs.indexOf(exp)))
              case _ =>
                args.callReference match {
                  case Some(Resolved(f: ScFunction, _)) => f.parameters.lift(args.exprs.indexOf(exp))
                  case _ => None
                }
            }
          case _ => None
        }
    }
  }

  private def conversionParameterOf(exp: ScExpression): Option[ScParameter] = {
    exp.getImplicitConversions._2
            .flatMap(_.asOptionOf(classOf[ScFunction]))
            .flatMap(_.parameters.headOption)
  }

  private def nonLiteralRangesIn(exp: ScExpression): Seq[TextRange] = {
    val literalRanges = exp.depthFirst.filterByType(classOf[ScLiteral]).map(_.getTextRange).toList
    val literalIndices = literalRanges.flatMap(r => List(r.getStartOffset, r.getEndOffset))
    val allIndices = exp.getTextRange.getStartOffset :: literalIndices ::: exp.getTextRange.getEndOffset :: Nil
    allIndices.grouped(2).map(it => new TextRange(it(0), it(1))).filterNot(_.isEmpty).toList
  }
}