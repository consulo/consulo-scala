package org.jetbrains.plugins.scala.findUsages

import consulo.annotation.component.ExtensionImpl

import consulo.project.util.query.QueryExecutorBase
import consulo.util.lang.StringUtil
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.psi.search.{RequestResultProcessor, SearchRequestCollector, UsageSearchContext}
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.util.PsiTreeUtil
import consulo.application.util.function.Processor
import org.jetbrains.annotations.{NotNull, Nullable}
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScConstructorInvocation, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition
import java.util.function.Predicate


/**
 * Finds usages of a type alias definition which refers to a class.
 *
 * The usages resolve *directly* to the class, so they are missed by the
 * standard reference search.
 *
 * @see [[org.jetbrains.plugins.scala.lang.resolve.processor.ConstructorResolveProcessor]]
 *
 * {{{
 *   class X()
 *   object A {
 *     type /*search*/alias = X
 *   }
 *   new A./*found*/alias()
 * }}}
 */
@ExtensionImpl
class TypeAliasUsagesSearcher extends QueryExecutorBase[PsiReference, ReferencesSearch.SearchParameters](true) {

  override def processQuery(@NotNull parameters: ReferencesSearch.SearchParameters, @NotNull consumer: Predicate[? >: PsiReference]): Unit = {
    val data = inReadAction {
      parameters.getElementToSearch match {
        case target @ ScalaPsiUtil.inNameContext(ta: ScTypeAliasDefinition) =>
          val nm = ta.name
          if (nm != null && !StringUtil.isEmptyOrSpaces(nm))
            Some((target, nm, parameters.getEffectiveSearchScope))
          else None
        case _ => None
      }
    }
    data match {
      case Some((target, name, scope)) =>
        val collector: SearchRequestCollector = parameters.getOptimizer
        val processor = new MyProcessor(target, null)
        collector.searchWord(name, scope, UsageSearchContext.IN_CODE, true, target, processor)
      case _ =>
    }
  }

  private class MyProcessor(myTarget: PsiElement, @Nullable prefix: String) extends RequestResultProcessor(myTarget, prefix) {
    override def processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Predicate[? >: PsiReference]): Boolean = inReadAction {
      element.parentOfType(classOf[ScConstructorInvocation], strict = false) match {
        case Some(cons) if PsiTreeUtil.isAncestor(cons.typeElement, element, false) =>
          element match {
            case resRef: ScReference => resRef.bind().flatMap(_.parentElement) match {
              case Some(`myTarget`) => consumer.test(resRef)
              case _ => true
            }
            case _ => true
          }
        case _ => true
      }
    }
  }
}
