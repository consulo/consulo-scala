package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.language.editor.template.TemplateManager
import consulo.language.editor.template.TemplateBuilder
import consulo.language.editor.template.TemplateBuilderFactory
import consulo.language.editor.template.EmptyExpression
import consulo.language.editor.highlight.HighlightManager
import consulo.language.editor.template.{ExpressionContext, Result, Template, TemplateManager, TextResult}
import consulo.language.editor.template.event.TemplateEditingAdapter
import consulo.language.editor.CommonDataKeys
import consulo.dataContext.DataContext
import consulo.undoRedo.CommandProcessor
import consulo.codeEditor.Editor
import consulo.codeEditor.EditorColors
import consulo.codeEditor.markup.RangeHighlighter
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.language.editor.refactoring.RefactoringBundle
import consulo.language.editor.refactoring.rename.RenameHandler
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.plugins.scala.lang.psi.api.expr.xml.ScXmlPairedTag
import org.jetbrains.plugins.scala.lang.refactoring.ScalaRefactoringActionHandler

import java.util

@ExtensionImpl
class XmlRenameHandler extends RenameHandler with ScalaRefactoringActionHandler {
  override def getActionTitleValue: LocalizeValue = LocalizeValue.localizeTODO("Rename XML tag")

  override def isAvailableOnDataContext(dataContext: DataContext): Boolean = {
    val editor = dataContext.getData(CommonDataKeys.EDITOR)
    if (editor == null || !editor.getSettings.isVariableInplaceRenameEnabled) return false

    val file = dataContext.getData(CommonDataKeys.PSI_FILE)
    if (file == null) return false
    val element = file.findElementAt(editor.getCaretModel.getOffset)

    if (element == null) return false

    element.getParent match {
      case _: ScXmlPairedTag => true
      case _ => false
    }
  }

  override def isRenaming(dataContext: DataContext): Boolean = isAvailableOnDataContext(dataContext)

  override def invoke(file: PsiFile)
                     (implicit project: Project, editor: Editor, dataContext: DataContext): Unit = {
    if (!isRenaming(dataContext)) return
    val element = file.findElementAt(editor.getCaretModel.getOffset)

    if (element != null) invoke(project, Array(element), dataContext)
  }

  override def invoke(elements: Array[PsiElement])
                     (implicit project: Project, dataContext: DataContext): Unit = {

    if (!isRenaming(dataContext) || elements == null || elements.length != 1) return

    val element: ScXmlPairedTag = if (elements(0) == null || !elements(0).getParent.isInstanceOf[ScXmlPairedTag]) return else
      elements(0).getParent.asInstanceOf[ScXmlPairedTag]
    if (element.getMatchedTag == null || element.getTagNameElement == null || element.getMatchedTag.getTagNameElement == null) return

    val editor = dataContext.getData(CommonDataKeys.EDITOR)
    val elementStartName = element.getTagName
    val rangeHighlighters = new util.ArrayList[RangeHighlighter]()
    val matchedRange = element.getMatchedTag.getTagNameElement.getTextRange

    def highlightMatched(): Unit = {
      val attributes = EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES

      HighlightManager.getInstance(editor.getProject).addOccurrenceHighlight(
        editor, matchedRange.getStartOffset, matchedRange.getEndOffset, attributes, 0, rangeHighlighters
      )

      rangeHighlighters.forEach { a =>
        a.setGreedyToLeft(true)
        a.setGreedyToRight(true)
      }
    }

    def rename(): Unit = {
      CommandProcessor.getInstance().executeCommand(project, () => {
        inWriteAction {
          val offset = editor.getCaretModel.getOffset
          val template = buildTemplate()
          editor.getCaretModel.moveToOffset(element.getParent.getTextOffset)

          TemplateManager.getInstance(project).startTemplate(editor, template, new TemplateEditingAdapter {
            override def templateFinished(template: Template, brokenOff: Boolean): Unit = {
              templateCancelled(template)
            }

            override def templateCancelled(template: Template): Unit = {
              val highlightManager = HighlightManager.getInstance(project)
              rangeHighlighters.forEach { a => highlightManager.removeSegmentHighlighter(editor, a) }
            }
          },
            (_: String, t: String) => !(t.length == 0 || t.charAt(t.length - 1) == ' '))

          highlightMatched()
          editor.getCaretModel.moveToOffset(offset)
        }
      }, RefactoringBundle.message("rename.title"), null)


    }

    def buildTemplate(): Template =  {
      val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(element.getParent)

      builder.replaceElement(element.getTagNameElement, "first", new EmptyExpression {
        override def calculateQuickResult(context: ExpressionContext): Result = new TextResult(Option(element.getTagName).getOrElse(elementStartName))
        override def calculateResult(context: ExpressionContext): Result = calculateQuickResult(context)
      }, true)
      builder.replaceElement(element.getMatchedTag.getTagNameElement, "second", "first", false)

      builder.buildInlineTemplate()
    }

    rename()
  }


}
