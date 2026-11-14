package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.lookup.{LookupElement, LookupElementBuilder}
import consulo.language.editor.template.TextResult
import com.intellij.codeInsight.template._
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.refactoring.namesSuggester.NameSuggester

@ExtensionImpl
final class ScalaSuggestVariableNameByTypeMacro extends ScalaMacro {

  override def getNameShort: String = "suggestVariableNameByTypeText"

  override def getPresentableName: String = ScalaCodeInsightBundle.message("macro.suggest.variable.name.by.type")

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = {
    val names = suggestNames(params)(using context)
    names.headOption.map(new TextResult(_)).orNull
  }

  private def suggestNames(params: Array[Expression])(implicit context: ExpressionContext): Seq[String] =
    params match {
      case Array(typeExpression) => suggestNames(typeExpression)(using context)
      case _                     => Nil
    }

  private def suggestNames(typeExpression: Expression)(implicit context: ExpressionContext): Seq[String] = {
    val typ = resolveScType(typeExpression)
    val names = typ.map(NameSuggester.suggestNamesByType(_))
    names.getOrElse(Nil)
  }

  override def calculateQuickResult(params: Array[Expression], context: ExpressionContext): Result = null

  override def calculateLookupItems(params: Array[Expression], context: ExpressionContext): Array[LookupElement] = {
    val names = suggestNames(params)(using context)
    if (names.size > 2) {
      names.map(LookupElementBuilder.create).toArray
    } else {
      null
    }
  }
}