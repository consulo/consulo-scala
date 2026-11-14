package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.lookup.LookupElement
import consulo.language.editor.template.{Expression, ExpressionContext}
import consulo.java.impl.JavaBundle
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.types.api.ExtractClass

@ExtensionImpl
final class ScalaComponentTypeOfMacro extends ScalaMacro {

  override def getNameShort: String = "componentTypeOf"

  override def getPresentableName: String = JavaBundle.message("macro.component.type.of.array")

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = params match {
    case Array(head) =>
      val maybeType = head.calculateResult(context) match {
        case ScalaTypeResult(scType) => Some(scType)
        case result => resultToScExpr(result)(using context)
      }

      maybeType.flatMap(arrayComponent)
        .map(ScalaTypeResult.apply)
        .orNull
    case _ => null
  }

  override def calculateLookupItems(params: Array[Expression], context: ExpressionContext): Array[LookupElement] = params match {
    case Array(head) =>
      head.calculateLookupItems(context) match {
        case null => null
        case outerItems =>
          outerItems.collect {
            case ScalaLookupItem(_, typeDefinition: ScTypeDefinition) => typeDefinition
          }.flatMap(_.`type`().toOption)
            .flatMap(arrayComponent)
            .collect {
              case ExtractClass(typeDefinition: ScTypeDefinition) => createLookupItem(typeDefinition)
            }
      }
    case _ => null
  }
}