package org.jetbrains.plugins.scala.findUsages.typeDef

import consulo.annotation.component.ExtensionImpl

import consulo.application.dumb.IndexNotReadyException
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.application.util.function.Processor
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper
import java.util.function.Predicate

@ExtensionImpl
class ObjectTraitReferenceSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {
  override def execute(queryParameters: ReferencesSearch.SearchParameters, consumer: Predicate[? >: PsiReference]): Boolean = {
    val scope = inReadAction(queryParameters.getEffectiveSearchScope)
    val element = queryParameters.getElementToSearch

    val toProcess = inReadAction {
      element match {
        case _ if !element.isValid => None
        case o: ScObject => Some((o, o.name))
        case wrapper@PsiClassWrapper(_: ScObject | _: ScTrait) => Some((wrapper, wrapper.getName))
        case _ => None
      }
    }
    toProcess.foreach{ case (elem, name) =>
      val processor = new TextOccurenceProcessor {
        override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
          val references = inReadAction(element.getReferences)
          for (ref <- references if ref.getRangeInElement.contains(offsetInElement)) {
            inReadAction {
              if (ref.isReferenceTo(elem) || ref.resolve() == elem) {
                if (!consumer.test(ref)) return false
              }
            }
          }
          true
        }
      }
      val helper: PsiSearchHelper = PsiSearchHelper.getInstance(queryParameters.getProject)
      try {
        helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      }
      catch {
        case _: IndexNotReadyException =>
        case ignore: AssertionError if ignore.getMessage.endsWith("has null range") =>
      }
    }
    true
  }
}
