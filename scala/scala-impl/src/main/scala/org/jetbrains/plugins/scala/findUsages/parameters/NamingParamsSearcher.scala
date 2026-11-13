package org.jetbrains.plugins.scala.findUsages.parameters

import consulo.language.psi.search.ReferencesSearchQueryExecutor

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.application.util.function.Processor
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScAssignment}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter

import scala.collection.mutable
import java.util.function.Predicate

@ExtensionImpl
class NamingParamsSearcher extends ReferencesSearchQueryExecutor {
  override def execute(queryParameters: ReferencesSearch.SearchParameters, consumer: Predicate[? >: PsiReference]): Boolean = {
    val project = queryParameters.getProject
    val scope = inReadAction(ScalaFilterScope(queryParameters))

    val data = inReadAction {
      queryParameters.getElementToSearch match {
        case e if !e.isValid => None
        case parameter: ScParameter => Some((parameter, parameter.name))
        case _ => None
      }
    }
    data match {
      case Some((parameter, name)) =>
        val collectedReferences = new mutable.HashSet[PsiReference]
        val processor = new TextOccurenceProcessor {
          override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
            val references = inReadAction(element.getReferences)
            for (ref <- references if ref.getRangeInElement.contains(offsetInElement) && !collectedReferences.contains(ref)) {
              ref match {
                case refElement: ScReference =>
                  inReadAction {
                    refElement.getParent match {
                      case assign: ScAssignment if assign.leftExpression == refElement &&
                        assign.getParent.isInstanceOf[ScArgumentExprList] =>
                        Option(refElement.resolve()) match {
                          case Some(`parameter`) => if (!consumer.test(ref)) return false
                          case Some(x: ScParameter) =>
                            ScalaPsiUtil.parameterForSyntheticParameter(x) match {
                              case Some(realParam) =>
                                if (realParam == parameter && !consumer.test(ref)) return false
                              case None =>
                            }
                          case _ =>
                        }
                      case _ =>
                    }
                  }
                case _ =>
              }
            }
            true
          }
        }
        val helper: PsiSearchHelper = PsiSearchHelper.getInstance(project)
        helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      case _ => true
    }
  }
}
