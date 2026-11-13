// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.scala.editor.selectioner

import consulo.language.editor.action.{AbstractWordSelectioner, ExtendWordSelectionHandlerBase}
import consulo.language.ast.ASTNode
import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.psi.{PsiComment, PsiElement, PsiWhiteSpace}
import org.jetbrains.plugins.scala.lang.scaladoc.parser.ScalaDocElementTypes

import java.util

/**
 * This is an analog of JavaDoc version [[com.intellij.codeInsight.editorActions.wordSelection.DocCommentSelectioner]].
 * We also extends [[LineCommentSelectioner]], that automatically allows us
 * selecting of the whole doc comment without definition to which the doc is attached
 *
 * @see [[com.intellij.codeInsight.editorActions.wordSelection.BlockCommentSelectioner]] (works for Scala automatically)
 * @see [[com.intellij.codeInsight.editorActions.wordSelection.LineCommentSelectioner]] (works for Scala automatically)
 */
// upstream: extended LineCommentSelectioner (impl class in Consulo), its `select` logic is inlined in `lineCommentSelect`
final class ScalaDocCommentSelectioner extends AbstractWordSelectioner {
  override def canSelect(e: PsiElement): Boolean = {
    val node = e.getNode
    node != null && {
      node.getElementType == ScalaDocElementTypes.SCALA_DOC_COMMENT
    }
  }

  override def select(element: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): util.List[TextRange] = {
    val node = element.getNode
    if (node == null)
      return null

    val result = lineCommentSelect(element, editorText, cursorOffset, editor)
    if (result == null)
      return null

    val mainContentRange = contentParagraphsRange(node)
    mainContentRange.foreach(result.add)

    result
  }

  private def lineCommentSelect(element: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): util.List[TextRange] = {
    val result = super.select(element, editorText, cursorOffset, editor)
    if (result == null)
      return null

    var firstComment = element
    var e = element
    var stop = false
    while (!stop && e.getPrevSibling != null) {
      e match {
        case _: PsiComment => firstComment = e
        case _: PsiWhiteSpace =>
        case _ => stop = true
      }
      if (!stop) e = e.getPrevSibling
    }

    var lastComment = element
    e = element
    stop = false
    while (!stop && e.getNextSibling != null) {
      e match {
        case _: PsiComment => lastComment = e
        case _: PsiWhiteSpace =>
        case _ => stop = true
      }
      if (!stop) e = e.getNextSibling
    }

    result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText,
      new TextRange(firstComment.getTextRange.getStartOffset, lastComment.getTextRange.getEndOffset)))
    result
  }

  private def contentParagraphsRange(node: ASTNode): Option[TextRange] = {
    val children = node.getChildren(null)
    for {
      firstParagraph <- children.find(_.getElementType == ScalaDocElementTypes.DOC_PARAGRAPH)
      lastParagraph <- children.findLast(_.getElementType == ScalaDocElementTypes.DOC_PARAGRAPH)
    } yield new TextRange(firstParagraph.getStartOffset, lastParagraph.getTextRange.getEndOffset)
  }
}