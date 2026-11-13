package org.jetbrains.plugins.scala.findUsages.vals

import consulo.language.psi.search.ReferencesSearchQueryExecutor

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.language.psi.{PsiElement, PsiReference}
import com.intellij.java.language.psi.PsiReferenceExpression
import consulo.application.util.function.Processor
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.fake.FakePsiMethod
import org.jetbrains.plugins.scala.lang.psi.light.{PsiTypedDefinitionWrapper, StaticPsiTypedDefinitionWrapper}
import java.util.function.Predicate

@ExtensionImpl
class JavaValsUsagesSearcher extends ReferencesSearchQueryExecutor {
  override def execute(queryParameters: ReferencesSearch.SearchParameters, consumer: Predicate[? >: PsiReference]): Boolean = {
    val scope = inReadAction(queryParameters.getEffectiveSearchScope)
    val element = queryParameters.getElementToSearch
    element match {
      case _ if inReadAction(!element.isValid) => true
      case scalaValue(vals, name) =>
        val processor = new TextOccurenceProcessor {
          override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
            val references = inReadAction(element.getReferences)
            for (ref <- references) {
              inReadAction {
                ref match {
                  case refElement: PsiReferenceExpression if ref.getRangeInElement.contains(offsetInElement) =>
                    refElement.resolve match {
                      case FakePsiMethod(`vals`) =>
                        if (!consumer.test(refElement)) return false
                      case StaticPsiTypedDefinitionWrapper(`vals`) =>
                        if (!consumer.test(refElement)) return false
                      case PsiTypedDefinitionWrapper(`vals`) =>
                        if (!consumer.test(refElement)) return false
                      case _ =>
                    }
                  case _ =>
                }
              }
            }
            true
          }
        }
        val helper: PsiSearchHelper = PsiSearchHelper.getInstance(queryParameters.getProject)
        helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      case wrapper@PsiTypedDefinitionWrapper(delegate) => //only this is added for find usages factory
        val name = inReadAction(wrapper.getName)

        val processor = new TextOccurenceProcessor {
          override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
            val references = inReadAction(element.getReferences)
            for (ref <- references) {
              inReadAction {
                ref match {
                  case refElement: PsiReferenceExpression if ref.getRangeInElement.contains(offsetInElement) =>
                    refElement.resolve match {
                      case otherWrapper@PsiTypedDefinitionWrapper(`delegate`) if otherWrapper.getName == name =>
                        if (!consumer.test(refElement)) return false
                      case otherWrapper@StaticPsiTypedDefinitionWrapper(`delegate`) if otherWrapper.getName == name =>
                        if (!consumer.test(refElement)) return false
                      case _ =>
                    }
                  case _ =>
                }
              }
            }
            true
          }
        }
        val helper: PsiSearchHelper = PsiSearchHelper.getInstance(queryParameters.getProject)
        helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      case _ => true
    }
  }

  private object scalaValue {
    def unapply(td: ScTypedDefinition): Option[(ScTypedDefinition, String)] = inReadAction {
      td.nameContext match {
        case _: ScValue | _: ScVariable | _: ScClassParameter if td.getName != "" => Some(td, td.getName)
        case _ => None
      }
    }
  }
}