package org.jetbrains.plugins.scala.findUsages.parameters

import consulo.find.FindUsagesOptions
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.psi.search.ReferencesSearch
import consulo.usage.UsageInfo
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScBindingPattern, ScExtractorPattern}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import java.util.function.Predicate

/**
 * {{{
 *   case class A(/*search*/a: Int)
 *   null match {
 *     case A(x) => /*found*/x
 *   }
 * }}}
 *
 * upstream: CustomUsageSearcher extension (not public API in Consulo), called from ScalaFindUsagesHandler instead
 */
object ExtractorParamsInExtractorPatternSearcher {
  def processElementUsages(element: PsiElement, processor0: Predicate[? >: UsageInfo], options: FindUsagesOptions): Boolean = {
    element match {
      case parameterOfClassWithIndex(cls, index) =>
        val scope = inReadAction(element.getUseScope)
        val correspondingSubpatternWithBindings = new SubPatternWithIndexBindings(index)

        val processor: Predicate[PsiReference] = {
          case correspondingSubpatternWithBindings(Seq(only)) =>
            ReferencesSearch.search(only, scope, false).forEach((t: PsiReference) => {
              inReadAction {
                processor0.test(new UsageInfo(t))
              }
            })
          case _ => true
        }
        ReferencesSearch.search(cls, scope, false).forEach(processor)
      case _ => true
    }
  }

  private object parameterOfClassWithIndex {
    def unapply(param: ScClassParameter): Option[(ScClass, Int)] = {
      inReadAction {
        if (!param.isValid) return None

        param.parentOfType(classOf[ScPrimaryConstructor]).flatMap {
          case pc@ScPrimaryConstructor.ofClass(cls) if cls.isCase =>
            pc.parameters.indexOf(param) match {
              case -1 => None
              case i => Some(cls, i)
            }
          case _ => None
        }
      }
    }
  }

  private class SubPatternWithIndexBindings(i: Int) {
    def unapply(ref: PsiReference): Option[Seq[ScBindingPattern]] = {
      inReadAction {
        ref.getElement.getParent match {
          case consPattern: ScExtractorPattern => consPattern.argPatterns.lift(i).map(_.bindings)
          case _ => None
        }
      }
    }
  }
}