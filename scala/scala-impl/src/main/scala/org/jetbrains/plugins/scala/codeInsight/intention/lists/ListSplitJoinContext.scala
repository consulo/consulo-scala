package org.jetbrains.plugins.scala.codeInsight.intention.lists

import consulo.document.util.TextRange
import consulo.language.codeStyle.CodeStyleManager
import consulo.language.psi.{PsiComment, PsiElement, PsiFile, PsiWhiteSpace}

import scala.collection.mutable

// upstream: IntelliJ platform com.intellij.openapi.editor.actions.lists (ListSplitJoinContext, DefaultListSplitJoinContext,
// CommaListSplitJoinContext); Consulo has no "listSplitJoinContext" extension point, so the base classes are plugin-local

final class ListWithElements(val list: PsiElement, val elements: Seq[PsiElement])

sealed trait JoinOrSplit

object JoinOrSplit {
  case object Join extends JoinOrSplit
  case object Split extends JoinOrSplit
}

/**
 * Describes the structure of split / join operations for list elements:
 * {{{
 *  test(p1, p2, p3)
 * }}}
 * and
 * {{{
 *  test(p1,
 *       p2,
 *       p3)
 * }}}
 */
trait ListSplitJoinContext {

  /** Checks if the split operation is available in the context */
  def isSplitAvailable(data: ListWithElements): Boolean

  /** Checks if the join operation is available in the context */
  def isJoinAvailable(data: ListWithElements): Boolean

  /** Extracts list and elements for the operations. Null means that the operation isn't available */
  def extractData(context: PsiElement): ListWithElements

  /** Text for the split operation, to show in the editor */
  def getSplitText(data: ListWithElements): String

  /** Text for the join operation, to show in the editor */
  def getJoinText(data: ListWithElements): String

  /** Reformat the range after processing */
  def reformatRange(file: PsiFile, rangeToAdjust: TextRange, split: JoinOrSplit): Unit

  /** Collects the ordered list (asc) of replacements to update document for the splitting */
  def getReplacementsForSplitting(data: ListWithElements): Seq[(TextRange, String)]

  /** Collects the ordered list (asc) of replacements to update document for the joining */
  def getReplacementsForJoining(data: ListWithElements): Seq[(TextRange, String)]
}

/**
 * Default implementation of split/join operations
 */
abstract class DefaultListSplitJoinContext extends ListSplitJoinContext {

  /**
   * Returns true, if the element is a separator of the list (e.g. comma).
   * If there are no explicit separators, it should be "false"
   */
  def isSeparator(element: PsiElement): Boolean

  /**
   * Returns true if the element is an acceptable part of the list. Shouldn't include separators
   */
  def isValidIntermediateElement(data: ListWithElements, element: PsiElement): Boolean = element.isInstanceOf[PsiWhiteSpace]

  /** Checks if we need to insert (to keep) new line at the start of list */
  def needHeadBreak(data: ListWithElements, firstElement: PsiElement, mode: JoinOrSplit): Boolean = false

  /** Checks if we need to insert (to keep) new line at the end of list */
  def needTailBreak(data: ListWithElements, lastElement: PsiElement, mode: JoinOrSplit): Boolean = false

  /** Tries to get the nearest prev line break (usually PsiWhiteSpace), should skip separators and comments */
  def prevBreak(data: ListWithElements, element: PsiElement): PsiElement = nextPrevBreak(data, element, next = false)

  /** Tries to get the nearest next line break (usually PsiWhiteSpace), should skip separators and comments */
  def nextBreak(data: ListWithElements, element: PsiElement): PsiElement = nextPrevBreak(data, element, next = true)

  //noinspection ScalaExtractStringToBundle
  override def getSplitText(data: ListWithElements): String = "Put elements on multiple lines"

  //noinspection ScalaExtractStringToBundle
  override def getJoinText(data: ListWithElements): String = "Put elements on one line"

  override def isSplitAvailable(data: ListWithElements): Boolean =
    data.elements.size > 1 && validateRange(data) && getReplacementsForSplitting(data).nonEmpty

  override def isJoinAvailable(data: ListWithElements): Boolean =
    data.elements.size > 1 && validateRange(data) && getReplacementsForJoining(data).nonEmpty

  override def reformatRange(file: PsiFile, rangeToAdjust: TextRange, split: JoinOrSplit): Unit =
    CodeStyleManager.getInstance(file.getProject).adjustLineIndent(file, rangeToAdjust)

  override def getReplacementsForSplitting(data: ListWithElements): Seq[(TextRange, String)] = {
    val elements = data.elements

    val replacements = mutable.ArrayBuffer.empty[(TextRange, String)]
    val firstElement = elements.head
    val lastElement = elements.last

    addHeadReplacementsForSplitting(data, replacements, firstElement)

    var hasInnerReplacement = false
    for (el <- elements.dropRight(1) if nextBreak(data, el) == null) {
      val offset = findOffsetForBreakAfter(data, el)
      replacements += new TextRange(offset, offset) -> "\n"
      hasInnerReplacement = true
    }

    //don't split if there are no nested elements for splitting
    if (!hasInnerReplacement) return Seq.empty

    addTailReplacementsForSplitting(data, replacements, lastElement)

    replacements.toSeq
  }

  override def getReplacementsForJoining(data: ListWithElements): Seq[(TextRange, String)] = {
    val elements = data.elements

    val replacements = mutable.ArrayBuffer.empty[(TextRange, String)]
    val firstElement = elements.head
    val lastElement = elements.last

    addHeadReplacementsForJoining(data, replacements, firstElement)

    for (current <- elements.dropRight(1)) {
      val break = nextBreak(data, current)
      if (break != null) {
        replacements += break.getTextRange -> " "
        addSiblingWhitespaceReplacement(replacements, break)
      }
    }

    addTailReplacementsForJoining(data, replacements, lastElement)

    replacements.toSeq
  }

  protected def addHeadReplacementsForSplitting(data: ListWithElements,
                                                replacements: mutable.Buffer[(TextRange, String)],
                                                firstElement: PsiElement): Unit = {
    if (needHeadBreak(data, firstElement, JoinOrSplit.Split) && prevBreak(data, firstElement) == null) {
      val offset = firstElement.getTextRange.getStartOffset
      replacements += new TextRange(offset, offset) -> "\n"
    }
  }

  protected def addHeadReplacementsForJoining(data: ListWithElements,
                                              replacements: mutable.Buffer[(TextRange, String)],
                                              firstElement: PsiElement): Unit = {
    if (!needHeadBreak(data, firstElement, JoinOrSplit.Join)) {
      val break = prevBreak(data, firstElement)
      if (break != null) {
        replacements += break.getTextRange -> getHeadBreakJoinReplacement(firstElement)
        addSiblingWhitespaceReplacement(replacements, break)
      }
    }
  }

  protected def addTailReplacementsForSplitting(data: ListWithElements,
                                                replacements: mutable.Buffer[(TextRange, String)],
                                                lastElement: PsiElement): Unit = {
    if (nextBreak(data, lastElement) == null && needTailBreak(data, lastElement, JoinOrSplit.Split)) {
      val offset = findOffsetForBreakAfter(data, lastElement)
      replacements += new TextRange(offset, offset) -> "\n"
    }
  }

  protected def addTailReplacementsForJoining(data: ListWithElements,
                                              replacements: mutable.Buffer[(TextRange, String)],
                                              lastElement: PsiElement): Unit = {
    if (!needTailBreak(data, lastElement, JoinOrSplit.Join)) {
      val break = nextBreak(data, lastElement)
      if (break != null) {
        replacements += break.getTextRange -> getTailBreakJoinReplacement(lastElement)
        addSiblingWhitespaceReplacement(replacements, break)
      }
    }
  }

  protected def getTailBreakJoinReplacement(lastElement: PsiElement): String = ""

  protected def getHeadBreakJoinReplacement(firstElement: PsiElement): String = ""

  protected final def findOffsetForBreakAfter(data: ListWithElements, element: PsiElement): Int = {
    val after = skipAcceptableElements(data, element)
    if (isSeparator(after)) {
      //accept comments after "," as a part of the break
      separatorOrNewLineComment(data, after).getTextRange.getEndOffset
    } else {
      after.getTextRange.getEndOffset
    }
  }

  protected final def hasSeparatorAfter(data: ListWithElements, lastElement: PsiElement): Boolean =
    isSeparator(skipAcceptableElements(data, lastElement))

  protected final def skipAcceptableElements(data: ListWithElements, element: PsiElement): PsiElement = {
    var curr = element
    var nextCandidate = curr.getNextSibling
    while (nextCandidate != null && isValidIntermediateElement(data, nextCandidate)) {
      curr = nextCandidate
      nextCandidate = curr.getNextSibling
    }
    val next = curr.getNextSibling

    if (next != null && isSeparator(next)) next else curr
  }

  private def addSiblingWhitespaceReplacement(replacements: mutable.Buffer[(TextRange, String)], lineBreak: PsiElement): Unit =
    lineBreak.getNextSibling match {
      //in some languages (GO) new lines and indents are not merged, so we need to remove it manually
      case whitespace: PsiWhiteSpace => replacements += whitespace.getTextRange -> ""
      case _ =>
    }

  private def nextPrevBreak(data: ListWithElements, element: PsiElement, next: Boolean): PsiElement = {
    def sibling(e: PsiElement) = if (next) e.getNextSibling else e.getPrevSibling

    var current = sibling(element)
    while (current != null && (isValidIntermediateElement(data, current) || isSeparator(current))) {
      if (current.isInstanceOf[PsiWhiteSpace] && current.textContains('\n')) return current
      current = sibling(current)
    }
    null
  }

  private def validateRange(data: ListWithElements): Boolean = {
    val elements = data.elements.toSet
    extractRange(data).forall(it => elements.contains(it) || isSeparator(it) || isValidIntermediateElement(data, it)) &&
      validateHeadElement(data, data.elements.head) &&
      validateTailElement(data, data.elements.last)
  }

  protected def validateHeadElement(data: ListWithElements, head: PsiElement): Boolean =
    validateHeadOrTail(data, head, next = false)

  protected def validateTailElement(data: ListWithElements, tail: PsiElement): Boolean =
    validateHeadOrTail(data, tail, next = true)

  /**
   * Reject split/join if before head / after tail there is an unsupported comment
   */
  private def validateHeadOrTail(data: ListWithElements, element: PsiElement, next: Boolean): Boolean = {
    def sibling(e: PsiElement) = if (next) e.getNextSibling else e.getPrevSibling

    var current = sibling(element)
    while (current != null) {
      current match {
        case comment: PsiComment =>
          if (!isValidIntermediateElement(data, comment)) return false
        case _: PsiWhiteSpace =>
        case _ =>
          return true
      }
      current = sibling(current)
    }
    true
  }

  private def extractRange(data: ListWithElements): Seq[PsiElement] = {
    val result = mutable.ArrayBuffer.empty[PsiElement]
    var collectStart = false
    val startElement = data.elements.head
    val endElement = data.elements.last

    var curr = data.list.getFirstChild
    var done = false
    while (curr != null && !done) {
      if (!collectStart) {
        if (startElement == curr) {
          collectStart = true
        }
      } else {
        result += curr
        if (curr == endElement) done = true
      }
      curr = curr.getNextSibling
    }
    result.toSeq
  }

  /**
   * Handle a specific case when a comment is started at the end of the parameter after comma and have several lines.
   * In this case inserting the line break after the comment is more preferable to keep the parameters aligned
   */
  private def separatorOrNewLineComment(data: ListWithElements, separator: PsiElement): PsiElement = {
    var next = separator
    while (true) {
      next.getNextSibling match {
        case whitespace: PsiWhiteSpace =>
          if (whitespace.textContains('\n')) return separator
          next = whitespace
        case comment: PsiComment if isValidIntermediateElement(data, comment) && comment.textContains('\n') =>
          return comment
        case _ =>
          return separator
      }
    }
    separator
  }
}

sealed trait TrailingComma

object TrailingComma {
  case object AddIfNotExists extends TrailingComma
  case object RemoveIfExists extends TrailingComma
  case object Ignore extends TrailingComma
}

abstract class CommaListSplitJoinContext extends DefaultListSplitJoinContext {
  override def isSeparator(element: PsiElement): Boolean =
    element != null && element.getFirstChild == null && element.textMatches(",")

  //noinspection ScalaExtractStringToBundle
  override def getSplitText(data: ListWithElements): String = "Put comma-separated elements on multiple lines"

  //noinspection ScalaExtractStringToBundle
  override def getJoinText(data: ListWithElements): String = "Put comma-separated elements on one line"

  private def addOrRemoveTrailingComma(data: ListWithElements,
                                       replacements: mutable.Buffer[(TextRange, String)],
                                       split: JoinOrSplit,
                                       lastElement: PsiElement): Unit =
    getTrailingComma(data, split, lastElement) match {
      case TrailingComma.AddIfNotExists =>
        if (!hasSeparatorAfter(data, lastElement)) {
          val offset = findOffsetForBreakAfter(data, lastElement)
          replacements += new TextRange(offset, offset) -> ","
        }
      case TrailingComma.RemoveIfExists =>
        val endElement = skipAcceptableElements(data, lastElement)
        if (isSeparator(endElement)) {
          replacements += endElement.getTextRange -> ""
        }
      case TrailingComma.Ignore =>
    }

  override protected def addTailReplacementsForSplitting(data: ListWithElements,
                                                         replacements: mutable.Buffer[(TextRange, String)],
                                                         lastElement: PsiElement): Unit = {
    addOrRemoveTrailingComma(data, replacements, JoinOrSplit.Split, lastElement)
    super.addTailReplacementsForSplitting(data, replacements, lastElement)
  }

  override protected def addTailReplacementsForJoining(data: ListWithElements,
                                                       replacements: mutable.Buffer[(TextRange, String)],
                                                       lastElement: PsiElement): Unit = {
    addOrRemoveTrailingComma(data, replacements, JoinOrSplit.Join, lastElement)
    super.addTailReplacementsForJoining(data, replacements, lastElement)
  }

  protected def getTrailingComma(data: ListWithElements, mode: JoinOrSplit, lastElement: PsiElement): TrailingComma =
    TrailingComma.Ignore
}
