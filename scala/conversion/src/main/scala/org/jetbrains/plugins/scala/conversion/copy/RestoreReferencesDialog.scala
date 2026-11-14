package org.jetbrains.plugins.scala.conversion.copy

import consulo.application.CommonBundle
import consulo.language.editor.localize.CodeInsightLocalize
import consulo.application.ApplicationManager
import consulo.colorScheme.EditorColorsScheme
import consulo.project.Project
import consulo.ui.ex.awt.{DialogWrapper, VerticalFlowLayout}
import consulo.ui.ex.awt.ScrollPaneFactory
import consulo.ui.ex.awt.{JBLabel, JBList}
import consulo.ui.ex.awt.UIUtil
import consulo.ui.ex.awt.UIUtil.ComponentStyle.SMALL
import consulo.ui.ex.awt.UIUtil.FontColor.BRIGHTER
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.lang.refactoring.Associations
import org.jetbrains.plugins.scala.project.ScalaFeatures

import java.awt._
import java.util.Collections
import javax.swing._
import scala.jdk.CollectionConverters.ListHasAsScala

class RestoreReferencesDialog(
  project: Project,
  bindings: Seq[Associations.Binding],
  features: ScalaFeatures,
  colorScheme: EditorColorsScheme
) extends DialogWrapper(project, true) {

  private val importedPathsArray = bindings.toArray

  override protected def getDimensionServiceKey = "#com.intellij.codeInsight.editorActions.RestoreReferencesDialog"

  @TestOnly
  def getBindings: Seq[Associations.Binding] = bindings

  private var myList: JList[Associations.Binding] = scala.compiletime.uninitialized
  private var mySelectedElements: java.util.List[Associations.Binding] = Collections.emptyList
  def getSelectedElements: Seq[Associations.Binding] = mySelectedElements.asScala.toSeq

  setTitle(CodeInsightLocalize.dialogImportOnPasteTitle2())

  init()

  myList.setSelectionInterval(0, importedPathsArray.length - 1)

  override protected def doOKAction(): Unit = {
    mySelectedElements = myList.getSelectedValuesList
    super.doOKAction()
  }

  override protected def createCenterPanel: JComponent = {
    val panel = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP))
    myList = new JBList(importedPathsArray*)
    myList.setCellRenderer(new BindingCellRenderer(features, colorScheme, project))
    panel.add(ScrollPaneFactory.createScrollPane(myList), BorderLayout.CENTER)
    panel.add(new JBLabel(CodeInsightLocalize.dialogPasteOnImportText2().get, SMALL, BRIGHTER), BorderLayout.NORTH)
    val buttonPanel = new JPanel(new VerticalFlowLayout)
    val okButton = new JButton(CommonBundle.getOkButtonText)


    if (!ApplicationManager.getApplication.isUnitTestMode) {
      // the root pane in tests is null
      getRootPane.setDefaultButton(okButton)
    }

    buttonPanel.add(okButton)
    val cancelButton = new JButton(CommonBundle.getCancelButtonText)
    buttonPanel.add(cancelButton)
    panel.setPreferredSize(new Dimension(500, 400))
    panel
  }
}