package org.jetbrains.plugins.scala.codeInsight.intention.lists

import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.editor.intention.{IntentionMetaData, LowPriorityAction, PsiElementBaseIntentionAction}
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.project.Project

// upstream: IntelliJ platform SplitJoinIntention / SplitLineIntention / JoinLinesIntention driven by "listSplitJoinContext"
// extensions; Consulo has no such extension point, so the intentions only use the Scala contexts
abstract class ScalaSplitJoinIntention extends PsiElementBaseIntentionAction with LowPriorityAction {
  protected def operation: JoinOrSplit

  protected def getIntentionText(splitJoinContext: ListSplitJoinContext, data: ListWithElements): String

  protected def getReplacements(splitJoinContext: ListSplitJoinContext, data: ListWithElements): Seq[(TextRange, String)]

  override final def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    ScalaListSplitJoinContexts.find(element, operation) match {
      case Some((splitJoinContext, data)) =>
        setText(LocalizeValue.of(getIntentionText(splitJoinContext, data)))
        true
      case None =>
        false
    }

  override final def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val (splitJoinContext, data) = ScalaListSplitJoinContexts.find(element, operation) match {
      case Some(found) => found
      case None => return
    }

    val documentManager = PsiDocumentManager.getInstance(project)
    val containingFile = element.getContainingFile
    val document = Option(editor).map(_.getDocument).orElse(Option(documentManager.getDocument(containingFile))) match {
      case Some(document) => document
      case None => return
    }
    val marker = document.createRangeMarker(data.list.getTextRange)

    val replacements = getReplacements(splitJoinContext, data)
    validateOrder(replacements)
    for ((range, text) <- replacements.reverseIterator) {
      document.replaceString(range.getStartOffset, range.getEndOffset, text)
    }
    documentManager.commitDocument(document)
    if (marker.isValid) {
      splitJoinContext.reformatRange(containingFile, marker.getTextRange, operation)
    }
  }

  protected def validateOrder(replacements: Seq[(TextRange, String)]): Unit =
    replacements.map(_._1).sliding(2).collectFirst {
      case Seq(prev, range) if range.getStartOffset < prev.getEndOffset => ()
    }.foreach { _ =>
      ScalaSplitJoinIntention.Log.error("Incorrect replacements order. The ranges must be sorted")
    }
}

object ScalaSplitJoinIntention {
  private val Log = Logger.getInstance(classOf[ScalaSplitJoinIntention])
}

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ScalaSplitLinesIntention", fileExtensions = Array("scala"), categories = Array("Scala"))
final class ScalaSplitLinesIntention extends ScalaSplitJoinIntention {
  //noinspection ScalaExtractStringToBundle
  def getFamilyName: String = "Put elements on multiple lines"

  override protected def operation: JoinOrSplit = JoinOrSplit.Split

  override protected def getIntentionText(splitJoinContext: ListSplitJoinContext, data: ListWithElements): String =
    splitJoinContext.getSplitText(data)

  override protected def getReplacements(splitJoinContext: ListSplitJoinContext, data: ListWithElements): Seq[(TextRange, String)] =
    splitJoinContext.getReplacementsForSplitting(data)
}

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ScalaJoinLinesIntention", fileExtensions = Array("scala"), categories = Array("Scala"))
final class ScalaJoinLinesIntention extends ScalaSplitJoinIntention {
  //noinspection ScalaExtractStringToBundle
  def getFamilyName: String = "Put elements on one line"

  override protected def operation: JoinOrSplit = JoinOrSplit.Join

  override protected def getIntentionText(splitJoinContext: ListSplitJoinContext, data: ListWithElements): String =
    splitJoinContext.getJoinText(data)

  override protected def getReplacements(splitJoinContext: ListSplitJoinContext, data: ListWithElements): Seq[(TextRange, String)] =
    splitJoinContext.getReplacementsForJoining(data)
}
