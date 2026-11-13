package org.jetbrains.plugins.scala.findUsages.factory

import consulo.find.FindUsagesOptions
import consulo.module.content.ProjectFileIndex
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import com.intellij.java.language.psi.{PsiClass, PsiMethod}
import consulo.language.psi.{PsiElement, PsiNamedElement}
import consulo.usage.UsageInfo
import consulo.application.util.function.Processor
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.findUsages.ExternalReferenceSearcher
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

import java.util
import java.util.Collections

/**
  * Find usages handler, which relies solely on compiler indices.
  *
  * See also: `org.jetbrains.plugins.scala.compiler.references.ScalaCompilerReferenceService`
  */
class CompilerIndicesFindUsagesHandler(
  e: PsiElement,
  config: ScalaFindUsagesConfiguration
) extends ScalaFindUsagesHandlerBase(e, config) {
  private val pfindex = ProjectFileIndex.getInstance(e.getProject)

  private def isInLibrary(element: PsiElement): Boolean = inReadAction {
    (for {
      file  <- element.getContainingFile.toOption
      vfile <- file.getVirtualFile.toOption
    } yield pfindex.isInLibrary(vfile)).getOrElse(false)
  }

  private def searchInCompilerIndices(
    e:         PsiNamedElement,
    processor: Processor[? >: UsageInfo]
  ): Boolean = {
    //noinspection ApiStatus
    ExternalReferenceSearcher
      .searchExternally(e)
      .forEach(ref => processor.process(new UsageInfo(ref)))
  }

  override def processElementUsages(
    element:   PsiElement,
    processor: Processor[? >: UsageInfo],
    options:   FindUsagesOptions
  ): Boolean = element match {
    case (named: PsiNamedElement) & ContainingClass(cls: ScTypeDefinition) if isInLibrary(element) =>
      val res = searchInCompilerIndices(named, processor)
      // this inheritor-traversing logic is already present in ScalaCompilerReferenceReader,
      // but as library jars are not indexed, we have to partially rely on standard searchers instead.
      if (res) {
        ClassInheritorsSearch.search(cls).forEach(inheritor => {
          val inherited = named match {
            case cls: PsiClass     => inheritor.findInnerClassByName(cls.name, true).toOption
            case method: PsiMethod => inheritor.findMethodBySignature(method, true).toOption
            case _                 => inheritor.findMethodsByName(named.name, true).find(m => !m.hasParameters)
          }
          inherited.forall(searchInCompilerIndices(_, processor))
        })
      } else false
    case named: PsiNamedElement => searchInCompilerIndices(named, processor)
    case _                      => false
  }

  override def processUsagesInText(
    element:     PsiElement,
    processor:   Processor[? >: UsageInfo],
    searchScope: GlobalSearchScope
  ): Boolean = true

  override def getStringsToSearch(element: PsiElement): util.Collection[String] =
    Collections.emptyList()

  override def isSearchForTextOccurrencesAvailable(
    psiElement:   PsiElement,
    isSingleFile: Boolean
  ): Boolean = false
}
