package org.jetbrains.plugins.scala.lang.refactoring.introduceParameter

import consulo.document.event.{DocumentEvent, DocumentListener}
import consulo.project.Project
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.ValidationInfo
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.{CheckBox, ComboBox, Label, TextBox}
import consulo.language.editor.refactoring.BaseRefactoringProcessor
import consulo.language.editor.refactoring.ui.JBListTableWitEditors
import consulo.ui.ex.awt.table.{JBTable, TableView}
import consulo.language.editor.ui.awt.EditorTextField
import consulo.ui.ex.awt.ToolbarDecorator
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, TypePresentationContext}
import org.jetbrains.plugins.scala.lang.refactoring._
import org.jetbrains.plugins.scala.lang.refactoring.changeSignature._
import org.jetbrains.plugins.scala.lang.refactoring.changeSignature.changeInfo.ScalaChangeInfo
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.ScalaFileType

import java.awt.BorderLayout
import java.util
import javax.swing._
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

class ScalaIntroduceParameterDialog(method: ScalaMethodDescriptor,
                                    introduceData: ScalaIntroduceParameterData)
                                   (implicit project: Project)
  extends ScalaChangeSignatureDialog(method, false) {

  private var paramNameField: TextBox = scala.compiletime.uninitialized
  private var typeCombobox: ComboBox[String] = scala.compiletime.uninitialized
  private var typeMap: util.LinkedHashMap[String, ScType] = scala.compiletime.uninitialized
  private var replaceOccurrencesChb: CheckBox = scala.compiletime.uninitialized
  private var defaultValuesUsagePanel: DefaultValuesUsagePanel = scala.compiletime.uninitialized
  private var defaultForIntroducedTextField: EditorTextField = scala.compiletime.uninitialized

  override def init(): Unit = {
    super.init()
    setTitle(ScalaIntroduceParameterHandler.REFACTORING_NAME)
  }

  override def createNorthPanel(): JComponent = {
    val panel = super.createNorthPanel() //to initialize fields
    val northPanel = HorizontalLayout.create()
      .add(createParamNamePanel())
      .add(createParamTypePanel())
      .add(TargetAWT.wrap(panel))
    panel.setVisible(false)

    val container = new JPanel(new BorderLayout())
    container.add(TargetAWT.to(northPanel), BorderLayout.NORTH)
    container.add(TargetAWT.to(createDefaultArgumentPanel()), BorderLayout.CENTER)
    container
  }

  override def isListTableViewSupported: Boolean = false

  override def createRefactoringProcessor(): BaseRefactoringProcessor = {
    val parameters = splittedItems.map(_.map(_.parameter))
    val changeInfo = ScalaChangeInfo(getVisibility, method.fun, getMethodName, returnType, parameters, isAddDefaultArgs, None)

    val newData = introduceData.copy(paramName = Option(paramNameField.getValue).getOrElse(""), tp = typeMap.get(typeCombobox.getValue),
      replaceAll = replaceOccurrencesChb.getValueOrError.booleanValue, defaultArg = defaultForIntroducedTextField.getText)

    changeInfo.introducedParameterData = Some(newData)
    new ScalaChangeSignatureProcessor(changeInfo)
  }

  override def createOptionsPanel(): JComponent = {
    val panel = super.createOptionsPanel()
    panel.setVisible(false)
    panel
  }

  override def customizeParametersTable(table: TableView[ScalaParameterTableModelItem]): Unit = {
    table.setSelection(util.Collections.emptyList())
  }

  override protected def createParametersListTable: JBListTableWitEditors = {
    new ScalaParametersListTable() {
      override protected def isRowEditable(row: Int): Boolean = false

      override protected def defaultText(item: ScalaParameterTableModelItem): String = ""
    }
  }

  override def getPreferredFocusedComponent: JComponent = TargetAWT.to(paramNameField).asInstanceOf[JComponent]

  protected override def doValidate(): ValidationInfo = null

  override protected def decorateParameterTable(table: JBTable): JPanel = {
    table.setCellSelectionEnabled(false)
    table.setRowSelectionAllowed(true)
    table.getSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    table.setSurrendersFocusOnKeystroke(true)
    @nowarn("cat=deprecation")
    val buttonsPanel: JPanel =
      ToolbarDecorator.createDecorator(table)
              .setMoveUpAction(upAction)
              .setMoveDownAction(downAction)
              .disableAddAction()
              .disableRemoveAction()
              .addExtraActions(createAddClauseButton(), createRemoveClauseButton())
              .createPanel
    myParametersTableModel.addTableModelListener(getSignatureUpdater: javax.swing.event.TableModelListener)
    buttonsPanel
  }

  @RequiredUIAccess
  private def createParamNamePanel(): consulo.ui.Component = {
    paramNameField = TextBox.create(introduceData.paramName)
    paramNameField.setVisibleLength(15)
    paramNameField.addValueListener { _ =>
      val newText: String = Option(paramNameField.getValue).getOrElse("")
      introducedParamTableItem.foreach(_.parameter.setName(newText))
      myParametersTableModel.fireTableDataChanged()
      parametersTable.updateUI()
      updateSignatureAlarmFired()
      getRefactorAction.setEnabled(newText.nonEmpty)
    }
    val paramNameLabel = Label.create(LocalizeValue.of(ScalaBundle.message("parameter.label.name")))
    paramNameLabel.setTarget(paramNameField)
    VerticalLayout.create()
      .add(paramNameLabel)
      .add(paramNameField)
  }

  @RequiredUIAccess
  private def createParamTypePanel(): consulo.ui.Component = {
    implicit val context: TypePresentationContext = method.getMethod
    typeMap = ScalaRefactoringUtil.getCompatibleTypeNames(introduceData.possibleTypes)
    typeCombobox = ComboBox.create(new util.ArrayList[String](typeMap.keySet))
    if (!typeMap.isEmpty) typeCombobox.setValueByIndex(0)
    val typeLabel = Label.create(LocalizeValue.of(ScalaBundle.message("parameter.label.type")))
    typeLabel.setTarget(typeCombobox)
    typeCombobox.addValueListener { _ =>
      val scType = typeMap.get(typeCombobox.getValue)
      introducedParamTableItem.foreach { item =>
        item.parameter.scType = scType
        item.typeText = scType.codeText
      }
      myParametersTableModel.fireTableDataChanged()
      parametersTable.updateUI()
      updateSignatureAlarmFired()
    }
    VerticalLayout.create()
      .add(typeLabel)
      .add(typeCombobox)
  }

  @RequiredUIAccess
  override def createDefaultArgumentPanel(): consulo.ui.Component = {
    defaultForIntroducedTextField = new EditorTextField(introduceData.defaultArg, project, ScalaFileType.INSTANCE)
    val label = Label.create(LocalizeValue.of(ScalaBundle.message("parameter.label.default.value")))
    val defaultValueField = TargetAWT.wrap(defaultForIntroducedTextField)
    defaultForIntroducedTextField.setOneLineMode(false)
    defaultForIntroducedTextField.setEnabled(true)
    defaultForIntroducedTextField.addDocumentListener(new DocumentListener {
      override def documentChanged(e: DocumentEvent): Unit = {
        introducedParamTableItem.foreach(_.parameter.defaultValue = defaultForIntroducedTextField.getText.trim)
      }
    })
    replaceOccurrencesChb = CheckBox.create(LocalizeValue.of(ScalaBundle.message("replace.all.occurrences")))
    replaceOccurrencesChb.setValue(false)
    replaceOccurrencesChb.setVisible(introduceData.occurrences.length > 1)
    defaultValuesUsagePanel = new DefaultValuesUsagePanel("")
    VerticalLayout.create()
      .add(label)
      .add(defaultValueField)
      .add(replaceOccurrencesChb)
      .add(defaultValuesUsagePanel.component)
  }

  private def introducedParamTableItem: Option[ScalaParameterTableModelItem] = {
    parameterItems.find(_.parameter.isIntroducedParameter)
  }

  override protected def getDefaultValuesPanel: DefaultValuesUsagePanel = defaultValuesUsagePanel
}
