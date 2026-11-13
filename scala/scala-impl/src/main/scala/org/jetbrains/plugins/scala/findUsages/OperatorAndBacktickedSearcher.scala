package org.jetbrains.plugins.scala.findUsages

import consulo.application.Application
import consulo.language.editor.TargetElementUtilExtender
import consulo.application.dumb.IndexNotReadyException
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.index.io.ID
import consulo.language.psi.stub.IdIndexEntry
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.scope.{GlobalSearchScope, LocalSearchScope}
import consulo.content.scope.SearchScope
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.language.psi.{PsiElement, PsiManager, PsiReference}
import consulo.annotation.access.{RequiredReadAction => RequiresReadLock}
import consulo.language.psi.stub.FileBasedIndex
import consulo.application.util.function.CommonProcessors
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil.BacktickedName.stripBackticks

import java.{util => ju}
import scala.jdk.CollectionConverters._
import java.util.function.Predicate

class OperatorAndBacktickedSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {

  override def execute(
    queryParameters: ReferencesSearch.SearchParameters,
    consumer: Predicate[? >: PsiReference]
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
        processElementsWithWord(queryParameters.getProject, processor, scope, name, UsageSearchContext.IN_CODE, caseSensitively = true)
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
    consumer: Predicate[? >: PsiReference]
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
        return consumer.test(reference)
      }

      val refResolvedElement = TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(),
        (it: TargetElementUtilExtender) => it.getReferenceOrReferencedElement(reference, ju.Collections.emptySet[String]())) match {
        case null => reference.resolve()
        case e => e
      }
      if (refResolvedElement == elementToSearch) {
        return consumer.test(reference)
      }

      true
    }
  }

  // upstream: subclass of PsiSearchHelperImpl overriding processCandidateFilesForText (impl class is not public API in Consulo):
  // candidate files are found by the whole text in the id index, then occurrences are searched in each file
  private def processElementsWithWord(project: Project,
                                      processor: TextOccurenceProcessor,
                                      searchScope: SearchScope,
                                      text: String,
                                      searchContext: Short,
                                      caseSensitively: Boolean): Boolean = {
    val helper = PsiSearchHelper.getInstance(project)
    val scope = searchScope match {
      case global: GlobalSearchScope => global
      case local => return helper.processElementsWithWord(processor, local, text, searchContext, caseSensitively)
    }
    if (!ScalaNamesValidator.isIdentifier(text)) return true

    val idIndex = ID.findByName[IdIndexEntry, Integer]("IdIndex")
    if (idIndex == null) return true

    val entries = ju.Collections.singletonList(new IdIndexEntry(text, caseSensitively))
    val collectProcessor = new CommonProcessors.CollectProcessor[VirtualFile]
    val condition: Predicate[Integer] = { (value: Integer) =>
      (value.intValue & searchContext) != 0
    }

    inReadAction {
      FileBasedIndex.getInstance.processFilesContainingAllKeys(idIndex, entries, scope, condition, collectProcessor)
    }

    val psiManager = PsiManager.getInstance(project)
    collectProcessor.getResults.asScala.forall { virtualFile =>
      inReadAction(Option(psiManager.findFile(virtualFile))).forall { psiFile =>
        helper.processElementsWithWord(processor, new LocalSearchScope(psiFile), text, searchContext, caseSensitively)
      }
    }
  }
}
