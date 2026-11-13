package org.jetbrains.plugins.scala.findUsages

import com.intellij.codeInsight.TargetElementUtilBase.TARGET_ELEMENT_EVALUATOR
import consulo.application.util.ReadActionProcessor
import consulo.application.dumb.IndexNotReadyException
import consulo.project.Project
import consulo.util.lang.function.Condition
import consulo.virtualFileSystem.VirtualFile
import com.intellij.psi.impl.cache.impl.id.IdIndex
import consulo.language.psi.stub.IdIndexEntry
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.scope.GlobalSearchScope
import consulo.language.psi.search.{TextOccurenceProcessor, UsageSearchContext}
import consulo.language.psi.{PsiElement, PsiReference}
import com.intellij.util.concurrency.annotations.RequiresReadLock
import consulo.util.collection.ContainerUtil
import consulo.language.psi.stub.FileBasedIndex
import consulo.application.util.function.{CommonProcessors, Processor}
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil.BacktickedName.stripBackticks

import java.{util => ju}

class OperatorAndBacktickedSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {

  override def execute(
    queryParameters: ReferencesSearch.SearchParameters,
    consumer: Processor[? >: PsiReference]
  ): Boolean = {
    val elementToSearch = queryParameters.getElementToSearch
    val scalaElementToSearch: ScNamedElement = elementToSearch match {
      case named: ScNamedElement => named
      case _ =>
        return true
    }

    val readActionResult = inReadAction {
      if (scalaElementToSearch.isValid) {
        val names = getNamesToProcess(scalaElementToSearch)
        val scope = ScalaFilterScope(queryParameters)
        Some((names, scope))
      } else None
    }

    if (readActionResult.isEmpty) {
      return true
    }
    val Some((namesToProcess, scope)) = readActionResult: @unchecked

    namesToProcess.foreach { name =>
      try {
        val processor = new MyTextOccurenceProcessor(scalaElementToSearch, consumer)
        val searchHelper = new ScalaPsiSearchHelper(queryParameters.getProject)
        searchHelper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
      } catch {
        case _: IndexNotReadyException =>
      }
    }

    true
  }

  @RequiresReadLock
  private def getNamesToProcess(elementToSearch: ScNamedElement): Set[String] = {
    var names = Set.empty[String]
    val name = elementToSearch.name
    names += name
    names += stripBackticks(name)
    names += stripBackticks(name).stripPrefix("unary_")
    names
  }

  private class MyTextOccurenceProcessor(
    elementToSearch: PsiElement,
    consumer: Processor[? >: PsiReference]
  ) extends TextOccurenceProcessor {

    override def execute(element: PsiElement, offsetInElement: Int): Boolean = {
      val references = inReadAction(element.getReferences)
      for {
        reference <- references
        if reference.getRangeInElement.contains(offsetInElement)
      } inReadAction {
        if (!processReference(reference))
          return false
      }

      true
    }

    private def processReference(reference: PsiReference): Boolean = {
      if (reference.isReferenceTo(elementToSearch)) {
        return consumer.process(reference)
      }

      val targetElementEvaluator = TARGET_ELEMENT_EVALUATOR.forLanguage(reference.getElement.getLanguage)
      val refResolvedElement = targetElementEvaluator.getElementByReference(reference, 0) match {
        case null => reference.resolve()
        case e => e
      }
      if (refResolvedElement == elementToSearch) {
        return consumer.process(reference)
      }

      true
    }
  }

  private class ScalaPsiSearchHelper(project: Project)
    extends PsiSearchHelperImpl(project) {

    override def processCandidateFilesForText(scope: GlobalSearchScope,
                                              searchContext: Short,
                                              caseSensitively: Boolean,
                                              text: String,
                                              processor: Processor[? >: VirtualFile]): Boolean = {
      if (!ScalaNamesValidator.isIdentifier(text)) return true

      val entries = ju.Collections.singletonList(new IdIndexEntry(text, caseSensitively))
      val collectProcessor = new CommonProcessors.CollectProcessor[VirtualFile]
      val condition: Condition[Integer] = { (value: Integer) =>
        (value.intValue & searchContext) != 0
      }

      inReadAction {
        FileBasedIndex.getInstance.processFilesContainingAllKeys(IdIndex.NAME, entries, scope, condition, collectProcessor)
      }

      val readActionProcessor: ReadActionProcessor[VirtualFile] = { (virtualFile: VirtualFile) =>
        processor.process(virtualFile)
      }
      ContainerUtil.process(collectProcessor.getResults, readActionProcessor)
    }
  }
}

