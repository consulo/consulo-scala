package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.{CheckBox, IntBox, Label}
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings.{MAX_PRESENTATION_LENGTH, MIN_PRESENTATION_LENGTH}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

import javax.swing.JComponent

// upstream: a GUI designer form (GeneralSettingsPanel.form)
final class GeneralSettingsPanel(obviousTypesLoader: () => Boolean, obviousTypesSaver: Boolean => Unit,
                                 preserveIndentsLoader: () => Boolean, preserveIndentsSaver: Boolean => Unit,
                                 presentationLengthLoader: () => Int, presentationLengthSaver: Int => Unit) {

  private var obviousTypes: Boolean = obviousTypesLoader()
  private var preserveIndents: Boolean = preserveIndentsLoader()
  private var presentationLength: Int = presentationLengthLoader()

  private var components: Option[(CheckBox, CheckBox, IntBox)] = None

  def showObviousTypes: Boolean = obviousTypes

  def getPreserveIntends: Boolean = preserveIndents

  def getPresentationLength: Int = presentationLength

  def isModified: Boolean =
    showObviousTypes != obviousTypesLoader() ||
      getPreserveIntends != preserveIndentsLoader() ||
      getPresentationLength != presentationLengthLoader()

  def saveSettings(): Unit = {
    obviousTypesSaver(showObviousTypes)
    preserveIndentsSaver(getPreserveIntends)
    presentationLengthSaver(getPresentationLength)
  }

  def reset(): Unit = {
    obviousTypes = obviousTypesLoader()
    preserveIndents = preserveIndentsLoader()
    presentationLength = presentationLengthLoader()
    components.foreach { case (obviousTypesCheckbox, preserveIndentsCheckBox, presentationLengthBox) =>
      obviousTypesCheckbox.setValue(obviousTypes)
      preserveIndentsCheckBox.setValue(preserveIndents)
      presentationLengthBox.setValue(presentationLength)
    }
  }

  @RequiredUIAccess
  lazy val getPanel: JComponent = {
    val obviousTypesCheckbox = CheckBox.create(LocalizeValue.of(ScalaCodeInsightBundle.message("show.types.even.if.they.are.obvious")))
    obviousTypesCheckbox.setValue(obviousTypes)
    obviousTypesCheckbox.addValueListener(e => obviousTypes = e.getValue)

    val preserveIndentsCheckBox = CheckBox.create(LocalizeValue.of(ScalaCodeInsightBundle.message("inlay.hints.respect.vertical.alignment")))
    preserveIndentsCheckBox.setValue(preserveIndents)
    preserveIndentsCheckBox.addValueListener(e => preserveIndents = e.getValue)

    val presentationLengthBox = IntBox.create(presentationLength)
    presentationLengthBox.setRange(MIN_PRESENTATION_LENGTH, MAX_PRESENTATION_LENGTH)
    presentationLengthBox.addValueListener(e => presentationLength = e.getValue)

    components = Some((obviousTypesCheckbox, preserveIndentsCheckBox, presentationLengthBox))

    val lengthLine = HorizontalLayout.create()
    lengthLine.add(Label.create(LocalizeValue.of(ScalaCodeInsightBundle.message("inlay.hints.limit.hint.length.to"))))
    lengthLine.add(presentationLengthBox)
    lengthLine.add(Label.create(LocalizeValue.of(ScalaCodeInsightBundle.message("inlay.hints.characters"))))

    val panel = VerticalLayout.create()
    panel.add(obviousTypesCheckbox)
    panel.add(preserveIndentsCheckBox)
    panel.add(lengthLine)
    TargetAWT.to(panel).asInstanceOf[JComponent]
  }
}
