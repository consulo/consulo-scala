package org.jetbrains.plugins.scala.annotator

import consulo.language.editor.highlight.HighlightingLevelManager
import consulo.module.content.ProjectFileIndex
import consulo.document.util.TextRange
import consulo.language.impl.psi.DummyHolder
import consulo.language.psi.{PsiComment, PsiElement, PsiFile, PsiWhiteSpace}
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.api.isUnitTestMode
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.settings.{ScalaHighlightingMode, ScalaProjectSettings}

import scala.collection.mutable

object HighlightingAdvisor {

  def shouldInspect(file: PsiFile): Boolean = {
    val hasScala = file.hasScalaPsi
    // TODO: we currently only check
    //  HighlightingLevelManager.shouldInspect ~ "Highlighting: All Problems" in code analyses widget,
    //  but we ignore HighlightingLevelManager.shouldInspect ~ "Highlighting: Syntax"
    //  we should review all our annotators and split them accordingly
    val shouldInspect = HighlightingLevelManager.getInstance(file.getProject).shouldInspect(file)
    hasScala && (shouldInspect || isUnitTestMode)
  }

  def isTypeAwareHighlightingEnabled(element: PsiElement): Boolean = {
    val settings = ScalaProjectSettings.getInstance(element.getProject)
    if (!settings.isTypeAwareHighlightingEnabled) {
      // Type-aware highlighting is disabled in settings (using the action toggle or the widget).
      return false
    }

    // upstream: IncompleteModelUtil.isIncompleteModel(element) check (no incomplete dependencies mode in Consulo)

    val file = element.getContainingFile
    val isEnabledForFile =
      file match {
        case scalaFile: ScalaFile =>
          (!isLibrarySource(scalaFile) || typeAwareHighlightingForScalaLibrarySourcesEnabled) &&
            !ScalaHighlightingMode.showCompilerErrorsScala3(scalaFile)
        case _: DummyHolder => true
        case _ => false
      }

    isEnabledForFile && !isInIgnoredRange(element, file)
  }

  @TestOnly
  //yes, i know this is quite ugly solution, but for now I decided to not overcomplicate it and just use static global variable
  var typeAwareHighlightingForScalaLibrarySourcesEnabled: Boolean = false

  private def isLibrarySource(file: ScalaFile): Boolean = {
    val vFile = file.getVirtualFile
    val index = ProjectFileIndex.getInstance(file.getProject)

    !file.isCompiled && vFile != null && index.isInLibrarySource(vFile)
  }

  private def isInIgnoredRange(element: PsiElement, file: PsiFile): Boolean = {
    val ignoredRanges = cachedInUserData("isInIgnoredRange.ignoredRanges", file, file.getManager.getModificationTracker) {
      val chars = file.charSequence
      val indexes = mutable.ArrayBuffer.empty[Int]
      var lastIndex = 0
      while (chars.indexOf("/*_*/", lastIndex) >= 0) {
        lastIndex = chars.indexOf("/*_*/", lastIndex) + 5
        indexes += lastIndex
      }
      if (indexes.isEmpty) Set.empty[TextRange] else {
        if (indexes.length % 2 != 0) indexes += chars.length

        var res = Set.empty[TextRange]
        for (i <- indexes.indices by 2) {
          res += new TextRange(indexes(i), indexes(i + 1))
        }
        res
      }
    }
    if (ignoredRanges.isEmpty || element.isInstanceOf[PsiFile]) false
    else {
      val noCommentWhitespace = element.children.find {
        case _: PsiComment | _: PsiWhiteSpace => false
        case _ => true
      }
      val offset =
        noCommentWhitespace
          .map(_.getTextOffset)
          .getOrElse(element.getTextOffset)
      ignoredRanges.exists(_.contains(offset))
    }
  }
}
