package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.lookup.{LookupElement, LookupElementBuilder}
import consulo.language.editor.template.{Expression, ExpressionContext, TextResult, Result}
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.indexing.search.searches.AnnotatedMembersSearch
import com.intellij.java.language.psi.{PsiClass, PsiMember}
import consulo.application.util.query.EmptyQuery
import consulo.application.util.query.Query
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

import scala.jdk.CollectionConverters._

@ExtensionImpl
final class ScalaAnnotatedMacro extends ScalaMacro {

  protected def getAnnotatedMembers(params: Array[Expression], context: ExpressionContext): Query[PsiMember] = {
    (params, context) match {
      case (null, _) | (_, null) =>
        EmptyQuery.getEmptyQuery[PsiMember]
      case (params, _) if params.length == 0 =>
        EmptyQuery.getEmptyQuery[PsiMember]
      case _ => //TODO should params.length always equal 1?
        val project = context.getProject
        val scope = GlobalSearchScope.allScope(project)
        Option(params.head.calculateResult(context)).flatMap(res => ScalaPsiManager.instance(project).
                getCachedClass(scope, res.toString)).map(AnnotatedMembersSearch.search(_, scope)).
                getOrElse(EmptyQuery.getEmptyQuery[PsiMember])
    }
  }

  override def getNameShort: String = "annotated"

  override def getPresentableName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("macro.annotated"))

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = {
    Option(getAnnotatedMembers(params, context).findFirst()).map(member => new TextResult(member match {
      case psiClass: PsiClass => psiClass.getQualifiedName
      case _ => member.getName
    })).orNull
  }

  override def calculateQuickResult(params: Array[Expression], context: ExpressionContext): Result = calculateResult(params, context)

  override def calculateLookupItems(params: Array[Expression], context: ExpressionContext): Array[LookupElement] = {
    val secondParamName = if (params.length > 1) params(1).calculateResult(context).toString else null
    val isShortName = secondParamName != null && !secondParamName.toBoolean
    val project = context.getProject
    val outerClass: Option[PsiClass] = Option(secondParamName).flatMap { secondParamName =>
      ScalaPsiManager.instance(project).getCachedClass(GlobalSearchScope.allScope(project), secondParamName)
    }
    getAnnotatedMembers(params, context).findAll()
      .asScala
      .filter(outerClass.isDefined && outerClass.contains(_))
      .map {
        case psiClass: PsiClass if !isShortName => psiClass.getQualifiedName
        case notClass => notClass.getName
      }
      .toSet[String]
      .map(LookupElementBuilder.create)
      .toArray
  }
}
