package org.jetbrains.plugins.scala.findUsages

import consulo.annotation.component.ExtensionImpl

import consulo.project.util.query.QueryExecutorBase
import com.intellij.java.language.psi.PsiMethod
import consulo.language.psi.PsiReference
import consulo.language.psi.search.SearchRequestCollector
import com.intellij.java.indexing.search.searches.MethodReferencesSearch
import consulo.language.psi.search.ReferencesSearch
import consulo.application.util.function.Processor
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import java.util.function.Predicate

/**
 * Searches for scala methods defined inside of local scopes, ie methods for which .getContainingClass == null.
 * These are not considered by [[com.intellij.psi.impl.search.MethodUsagesSearcher]]
 */
@ExtensionImpl
class NonMemberMethodUsagesSearcher extends QueryExecutorBase[PsiReference, MethodReferencesSearch.SearchParameters] {
  override def processQuery(@NotNull p: MethodReferencesSearch.SearchParameters, @NotNull consumer: Predicate[? >: PsiReference]): Unit = {
    inReadAction {
      val method: PsiMethod = p.getMethod
      val collector: SearchRequestCollector = p.getOptimizer
      val newConsumer = new Processor[PsiReference] {
        override def process(t: PsiReference): Boolean = {
          if (method.isConstructor) return true
          consumer.test(t)
        }
      }
      ReferencesSearch.searchOptimized(method, ScalaFilterScope(p), false, collector, newConsumer)
    }
  }
}

