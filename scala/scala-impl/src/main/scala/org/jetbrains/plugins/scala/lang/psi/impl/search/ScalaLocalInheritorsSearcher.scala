package org.jetbrains.plugins.scala.lang.psi.impl.search

import com.intellij.java.indexing.search.searches.ClassInheritorsSearchExecutor

import java.util.function.Predicate
import consulo.annotation.component.ExtensionImpl

import consulo.project.util.query.QueryExecutorBase
import consulo.application.progress.ProgressManager
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.{PsiFile, PsiManager}
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch.SearchParameters
import consulo.language.psi.scope.{LocalSearchScope, PsiSearchScopeUtil}
import consulo.content.scope.SearchScope
import consulo.application.util.function.Processor
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScTemplateDefinition, ScTypeDefinition}

/**
  * @see [[ScalaDirectClassInheritorsSearcher]]<br>
  * TODO: show inheritors via gutter doesn't work in Scratch files (see IDEA-313012)
  */
@ExtensionImpl
class ScalaLocalInheritorsSearcher extends QueryExecutorBase[PsiClass, ClassInheritorsSearch.SearchParameters] with ClassInheritorsSearchExecutor {

  override def processQuery(params: SearchParameters, consumer: Predicate[? >: PsiClass]): Unit = {
    val clazz = params.getClassToProcess

    val scope = params.getScope
    val (_, virtualFiles) = scope match {
      case local: LocalSearchScope if clazz.isInstanceOf[ScalaPsiElement] =>
        // upstream: LocalSearchScope.getVirtualFiles
        val files = inReadAction {
          local.getScope.toSeq.flatMap(e => Option(e.getContainingFile).flatMap(f => Option(f.getVirtualFile))).distinct
        }
        (local, files)
      case _ =>
        return
    }

    val project = clazz.getProject

    for (virtualFile <- virtualFiles) {
      ProgressManager.checkCanceled()
      var continue = true
      inReadAction {
        if (continue) {
          val psiFile: PsiFile = PsiManager.getInstance(project).findFile(virtualFile)
          if (psiFile != null) {
            psiFile.depthFirst().foreach {
              case ntd: ScNewTemplateDefinition if ntd.extendsBlock.templateBody.isEmpty =>
              case td: ScTemplateDefinition if continue =>
                if (td.isInheritor(clazz, true) && checkCandidate(td, params, scope))
                  continue = consumer.test(td)
              case _ =>
            }
          }
        }
      }
    }
  }

  private def checkCandidate(
    candidate: PsiClass,
    parameters: ClassInheritorsSearch.SearchParameters,
    searchScope: SearchScope
  ): Boolean = {
    ProgressManager.checkCanceled()
    if (!PsiSearchScopeUtil.isInScope(searchScope, candidate))
      false
    else candidate match {
      case _: ScNewTemplateDefinition => true
      case td: ScTypeDefinition =>
        parameters.getNameCondition.test(td.name)
    }
  }
}
