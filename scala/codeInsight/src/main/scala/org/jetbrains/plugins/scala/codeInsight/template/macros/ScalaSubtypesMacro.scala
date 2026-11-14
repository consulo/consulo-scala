package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.lookup.LookupElement
import consulo.language.editor.template.{Expression, ExpressionContext, Result}
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.types.api.ExtractClass

import scala.jdk.CollectionConverters._

@ExtensionImpl
final class ScalaSubtypesMacro extends ScalaMacro {

  override def getNameShort: String = "subtypes"

  override def getPresentableName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("macro.subtypes"))

  override def getDefaultValue: String = ScalaMacro.DefaultValue

  override def calculateResult(expressions: Array[Expression], context: ExpressionContext): Result =
    expressions match {
      case Array(head) => head.calculateResult(context)
      case _ => null
    }

  override def calculateQuickResult(expressions: Array[Expression], context: ExpressionContext): Result =
    calculateResult(expressions, context)

  override def calculateLookupItems(expressions: Array[Expression], context: ExpressionContext): Array[LookupElement] =
    calculateResult(expressions, context) match {
      case ScalaTypeResult(ExtractClass(typeDefinition: ScTypeDefinition)) =>
        val inheritors = ClassInheritorsSearch.search(
          typeDefinition,
          GlobalSearchScope.projectScope(context.getProject),
          true
        ).findAll().asScala

        inheritors.collect {
          case definition: ScTypeDefinition => createLookupItem(definition)
        }.toArray
      case _ => LookupElement.EMPTY_ARRAY
    }
}
