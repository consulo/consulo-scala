package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch

import consulo.project.Project
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.language.psi.{PsiElement, PsiReference}
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.Search.Pipeline.ShouldProcess
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.Search.{Method, SearchMethodResult}
import org.jetbrains.plugins.scala.util.ScalaUsageNamesUtil

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.CollectionHasAsScala

private[cheapRefSearch] final class TextSearch(
  override val shouldProcess: ShouldProcess,
  project: Project
) extends Method {

  private val psiSearchHelper: PsiSearchHelper = PsiSearchHelper.getInstance(project)

  override def searchForUsages(ctx: Search.Context): SearchMethodResult = {
    val buffer = new ConcurrentLinkedQueue[ElementUsage]()
    var didExitBeforeExhaustion = false
    val searchedElement = ctx.element

    val processor = new TextOccurenceProcessor {
      override def execute(foundElement: PsiElement, offsetInElement: Int): Boolean = {
        if (foundElement.getContainingFile == searchedElement.getContainingFile) {
          true
        } else {
          val found = foundElement match {
            case _: PsiReference => true
            // upstream: also UAST reference expressions of other JVM languages (no UReferenceExpression in Consulo)
            case _ => false
          }

          if (found) {
            val usage = ElementUsageWithKnownReference(foundElement, searchedElement)
            buffer.add(usage)
            if (ctx.canExit(usage)) {
              didExitBeforeExhaustion = true
              false // exit
            } else {
              true // continue
            }
          } else {
            true // continue
          }
        }
      }
    }

    val useScope = psiSearchHelper.getUseScope(searchedElement)
    val stringsToSearch = ScalaUsageNamesUtil.getStringsToSearch(searchedElement).asScala.toSeq
    stringsToSearch
      .iterator
      .takeWhile(_ => !didExitBeforeExhaustion)
      .foreach { name =>
        psiSearchHelper.processElementsWithWord(processor, useScope, name,
          (UsageSearchContext.IN_CODE | UsageSearchContext.IN_FOREIGN_LANGUAGES).toShort, true)
      }

    new SearchMethodResult(buffer.asScala.toSeq, didExitBeforeExhaustion)
  }
}
