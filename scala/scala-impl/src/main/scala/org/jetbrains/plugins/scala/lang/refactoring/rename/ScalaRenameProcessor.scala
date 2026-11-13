package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.content.scope.SearchScope
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.refactoring.rename.ScalaRenameUtil.isAliased
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import java.util
import scala.jdk.CollectionConverters._

trait ScalaRenameProcessor { this: RenamePsiElementProcessor =>

  // upstream: searchScope parameter (Consulo RenamePsiElementProcessor.findReferences has no scope, the use scope is searched)
  override def findReferences(element: PsiElement,
                              searchInCommentsAndStrings: Boolean): util.Collection[PsiReference] = {

    val allRefs = ReferencesSearch.search(element, element.getUseScope).findAll()
    val filtered = allRefs.asScala.filterNot(isAliased).filterNot(ScalaRenameProcessor.isIndirectReference(_, element))
    val references = new util.ArrayList[PsiReference](filtered.asJavaCollection)
    ScalaGivenRenameUtil.syntheticGivenNameReferences(element, references).foreach(references.add)
    references
  }

  override def setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean): Unit = {
    ScalaApplicationSettings.getInstance().RENAME_SEARCH_IN_NON_CODE_FILES = enabled
  }

  override def isToSearchForTextOccurrences(element: PsiElement): Boolean = {
    ScalaApplicationSettings.getInstance().RENAME_SEARCH_IN_NON_CODE_FILES
  }

  override def setToSearchInComments(element: PsiElement, enabled: Boolean): Unit = {
    ScalaApplicationSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_AND_STRINGS = enabled
  }

  override def isToSearchInComments(element: PsiElement): Boolean = {
    ScalaApplicationSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_AND_STRINGS
  }
}

object ScalaRenameProcessor {
  private def isIndirectReference(ref: PsiReference, element: PsiElement): Boolean = ref match {
    case scRef: ScReference => scRef.isIndirectReferenceTo(ref.resolve(), element)
    case _ => false
  }
}
