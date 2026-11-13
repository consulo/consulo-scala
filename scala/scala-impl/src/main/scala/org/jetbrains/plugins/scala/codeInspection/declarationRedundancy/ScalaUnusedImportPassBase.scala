package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.highlight.TextEditorHighlightingPass
import consulo.language.editor.rawHighlight.{HighlightDisplayKey, HighlightInfo, HighlightInfoType}
import consulo.language.editor.inspection.HighlightInfoTypeSeverityByKey
import consulo.codeEditor.CodeInsightColors
import consulo.language.editor.highlight.UpdateHighlightersUtil
import consulo.language.editor.intention.IntentionAction
import consulo.document.Document
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions.Parent
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.usages.{ImportExprUsed, ImportSelectorUsed, ImportUsed, ImportWildcardSelectorUsed}

import java.util
import scala.annotation.nowarn

trait ScalaUnusedImportPassBase { self: TextEditorHighlightingPass =>
  def file: PsiFile
  def document: Document

  def collectHighlightings(unusedImports: Iterable[ImportUsed]): Iterable[HighlightInfo] =
    unusedImports
      // Do not highlight imports that were marked as unused by compiler and highlighted in
      // org.jetbrains.plugins.scala.compiler.highlighting.ExternalHighlighters.applyHighlighting
      // to avoid duplicated highlightings
      .filterNot(imp => imp.isAlwaysUsed || imp.markedAsUnusedByCompiler)
      .flatMap { (imp: ImportUsed) =>
        val psiOption: Option[PsiElement] = imp match {
          case ImportExprUsed(expr@Parent(importStmt: ScImportStmt)) if !PsiTreeUtil.hasErrorElements(expr) =>
            if (importStmt.importExprs.size == 1) Some(importStmt)
            else Some(expr)
          case ImportSelectorUsed(sel) => Some(sel)
          case ImportWildcardSelectorUsed(e) if e.selectors.nonEmpty => Some(e.wildcardElement.get)
          case ImportWildcardSelectorUsed(e) if !PsiTreeUtil.hasErrorElements(e) => Some(e.getParent)
          case _ => None
        }

        val qName = imp.qualName

        psiOption.flatMap { psi =>
          val highlightInfoBuilder =
            HighlightInfo.newHighlightInfo(unusedSymbolType)
              .range(psi)
              .descriptionAndTooltip(ScalaInspectionBundle.message("unused.import.statement"))

          getFixes.foreach(action => highlightInfoBuilder.registerFix(action))
          qName.foreach(name => highlightInfoBuilder.registerFix(new MarkImportAsAlwaysUsed(name)))
          // can be null if was rejected by com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
          Option(highlightInfoBuilder.create())
        }
      }

  // upstream: HighlightInfoType.UNUSED_SYMBOL (severity of the "unused" tool)
  private def unusedSymbolType: HighlightInfoType =
    Option(HighlightDisplayKey.find("unused"))
      .map(key => new HighlightInfoTypeSeverityByKey(key, CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES): HighlightInfoType)
      .getOrElse(HighlightInfoType.RAW_UNUSED_SYMBOL)

  protected def highlightAll(highlights: util.Collection[HighlightInfo]): Unit = {
    UpdateHighlightersUtil.setHighlightersToEditor(file.getProject, document, 0,
      file.getTextLength, highlights, getColorsScheme, getId): @nowarn("cat=deprecation")
  }

  protected def getFixes: List[IntentionAction]
}
