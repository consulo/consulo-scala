package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.codeEditor.markup.HighlighterLayer
import consulo.codeEditor.{CodeInsightColors, Editor, EditorFactory}
import consulo.document.event.{DocumentEvent, DocumentListener}
import consulo.language.Language
import consulo.language.editor.inlay.{InlayParameterHintsProvider, ParameterNameHintsSettings}
import consulo.language.editor.localize.CodeInsightLocalize
import consulo.language.editor.ui.awt.EditorTextField
import consulo.language.plain.PlainTextFileType
import consulo.localize.LocalizeValue
import consulo.platform.base.localize.CommonLocalize
import consulo.project.Project
import consulo.ui.Hyperlink
import consulo.ui.ex.awt.{BorderLayoutPanel, DialogWrapper, VerticalFlowLayout}
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints

import java.awt.{Component, Dimension}
import java.util.regex.{Pattern, PatternSyntaxException}
import javax.swing.{JComponent, JLabel, JPanel}
import scala.jdk.CollectionConverters.SetHasAsJava

/**
 * A copy of [[consulo.ide.impl.idea.codeInsight.hints.settings.ExcludeListDialog]] that takes [[provider]] as a parameter
 * instead of looking it up statically.
 */
private class ExcludeListDialog(language: Language, provider: InlayParameterHintsProvider, patternToAdd: String = null) extends DialogWrapper(null: Project) {
  private var myEditor: EditorTextField = scala.compiletime.uninitialized
  private var myPatternsAreValid = true

  locally {
    setTitle(CodeInsightLocalize.settingsInlayParameterHintsExcludeList())
    init()
  }

  override def createCenterPanel(): JComponent = {
    val blackList = getLanguageExcludeList
    val finalText = if (patternToAdd != null) blackList + "\n" + patternToAdd else blackList
    val editorTextField = createExcludeListEditorField(finalText)
    editorTextField.setAlignmentX(Component.LEFT_ALIGNMENT)
    editorTextField.addDocumentListener(new DocumentListener {
      override def documentChanged(e: DocumentEvent): Unit = {
        updateOkEnabled(editorTextField)
      }
    })
    updateOkEnabled(editorTextField)

    myEditor = editorTextField

    val layoutPanel = new BorderLayoutPanel()
    val resetLink = Hyperlink.create(CommonLocalize.linkReset(), _ => setLanguageExcludelistToDefault())

    layoutPanel.addToTop(TargetAWT.to(resetLink))
    layoutPanel.addToCenter(editorTextField)

    val bottomPanel = new JPanel(new VerticalFlowLayout())
    layoutPanel.addToBottom(bottomPanel)

    val comment = baseLanguageComment
    if (comment.isNotEmpty) {
      bottomPanel.add(new JLabel(comment.get))
    }

    val explanationHTML = provider.getBlacklistExplanationHTML
    if (explanationHTML.isNotEmpty) {
      bottomPanel.add(new JLabel(explanationHTML.get))
    }

    layoutPanel
  }

  private def baseLanguageComment: LocalizeValue =
    Option(provider.getBlackListDependencyLanguage)
      .map(it => CodeInsightLocalize.inlayHintsBaseExcludeListDescription(it.getDisplayName))
      .getOrElse(LocalizeValue.empty())

  private def setLanguageExcludelistToDefault(): Unit = {
    val defaultExcludeList = provider.getDefaultBlackList
    myEditor.setText(StringUtil.join(defaultExcludeList, "\n"))
  }

  private def updateOkEnabled(editorTextField: EditorTextField): Unit = {
    val text = editorTextField.getText
    val invalidLines = getExcludeListInvalidLineNumbers(text)
    myPatternsAreValid = invalidLines.isEmpty

    getOKAction.setEnabled(myPatternsAreValid)

    val editor = editorTextField.getEditor
    if (editor != null) {
      highlightErrorLines(invalidLines, editor)
    }
  }

  override def doOKAction(): Unit = {
    super.doOKAction()
    val excludeList = myEditor.getText
    storeExcludeListDiff(excludeList)
  }

  // upstream: the settings key is getLanguageForSettingKey(language) - the Scala provider is not registered, so it is the language
  private def storeExcludeListDiff(text: String): Unit = {
    val updatedExcludeList = text.split("\n").map(_.trim).filter(_.nonEmpty).toSet.asJava

    val defaultExcludeList = provider.getDefaultBlackList
    val diff = ParameterNameHintsSettings.Diff.build(defaultExcludeList, updatedExcludeList)
    ParameterNameHintsSettings.getInstance.setExcludeListDiff(language, diff)
    ImplicitHints.updateInAllEditors()
  }

  private def getLanguageExcludeList: String = {
    val diff = ParameterNameHintsSettings.getInstance().getExcludeListDiff(language)
    val excludeList = diff.applyOn(provider.getDefaultBlackList)
    StringUtil.join(excludeList, "\n")
  }

  private def createExcludeListEditorField(text: String): EditorTextField = {
    val document = EditorFactory.getInstance().createDocument(text)
    val field = new EditorTextField(document, null, PlainTextFileType.INSTANCE, false, false)
    field.setPreferredSize(new Dimension(400, 350))
    field.addSettingsProvider { editor =>
      editor.setVerticalScrollbarVisible(true)
      editor.setHorizontalScrollbarVisible(true)
      editor.getSettings.setAdditionalLinesCount(2)
      highlightErrorLines(getExcludeListInvalidLineNumbers(text), editor)
    }
    field
  }

  private def highlightErrorLines(lines: Seq[Int], editor: Editor): Unit = {
    val document = editor.getDocument
    val totalLines = document.getLineCount

    val model = editor.getMarkupModel
    model.removeAllHighlighters()
    lines
      .filter(_ < totalLines)
      .foreach(line => model.addLineHighlighter(CodeInsightColors.ERRORS_ATTRIBUTES, line, HighlighterLayer.ERROR))
  }

  private def getExcludeListInvalidLineNumbers(text: String): Seq[Int] =
    text.split("\n", -1).toSeq.zipWithIndex.collect {
      case (line, index) if line.trim.nonEmpty && !isValidPattern(line.trim) => index
    }

  private def isValidPattern(pattern: String): Boolean =
    try {
      Pattern.compile(pattern)
      true
    } catch {
      case _: PatternSyntaxException => false
    }
}
