package org.jetbrains.plugins.scala.worksheet.runconfiguration

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import java.{util => ju}

import consulo.language.editor.gutter.{LineMarkerInfo, LineMarkerProvider}
import consulo.platform.base.icon.PlatformIconGroup
import consulo.codeEditor.markup.GutterIconRenderer
import consulo.document.Document
import consulo.codeEditor.EditorFactory
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiComment, PsiDocumentManager, PsiElement, PsiWhiteSpace}
import consulo.language.editor.Pass
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.worksheet.WorksheetFile

import scala.jdk.CollectionConverters._

@ExtensionImpl
class WorksheetLineMarkerProvider extends LineMarkerProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo[?] = null

  override def collectSlowLineMarkers(elements: ju.List[PsiElement],
                                      result: ju.Collection[LineMarkerInfo[?]]): Unit =
    // assuming that all elements are from the same file
    for {
      firstElement <- elements.iterator.asScala.nextOption()
      scalaFile    <- worksheetFile(firstElement)
      marker       <- lineMarkerInfo(elements, scalaFile)
    } result.add(marker)

  private def worksheetFile(element: PsiElement): Option[WorksheetFile] =
    element.getContainingFile match {
      case file: WorksheetFile if file.isRepl =>
        Some(file)
      case _ =>
        None
    }

  private def lineMarkerInfo(elements: ju.List[? <: PsiElement], scalaFile: ScalaFile): Option[LineMarkerInfo[PsiElement]] = {
    val project = scalaFile.getProject
    for {
      document          <- Option(PsiDocumentManager.getInstance(project).getCachedDocument(scalaFile))
      editor            <- EditorFactory.getInstance().getEditors(document, project).headOption
      lastProcessedLine <- WorksheetCache.getInstance(project).getLastProcessedIncremental(editor)
      element           <- findMarkerAnchorElement(elements, document, lastProcessedLine)
    } yield createArrowMarker(element)
  }

  private def findMarkerAnchorElement(elements: ju.List[? <: PsiElement],
                                      document: Document,
                                      lastProcessedLine: Int): Option[PsiElement] =
    elements.iterator.asScala
      .filter(!isEmpty(_))
      .find(el => document.getLineNumber(el.startOffset) == lastProcessedLine)

  private def isEmpty(element: PsiElement): Boolean = element match {
    case _: PsiComment | _: PsiWhiteSpace    => true
    case empty if empty.getTextRange.isEmpty => true
    case _                                   => false
  }

  private def createArrowMarker(psiElement: PsiElement): LineMarkerInfo[PsiElement] = {
    val leaf = PsiTreeUtil.firstChild(psiElement)
    new LineMarkerInfo[PsiElement](
      leaf,
      leaf.getTextRange,
      PlatformIconGroup.actionsForward(),
      Pass.LINE_MARKERS,
      // upstream: no tooltip, accessible name "accessibility.worksheet.arrow"
      (_: PsiElement) => null,
      null,
      GutterIconRenderer.Alignment.RIGHT
    )
  }
}