package org.jetbrains.plugins.scala.codeInsight.hints.methodChains

import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.{CheckBox, IntBox, Label}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

import javax.swing.JComponent

// upstream: a GUI designer form (ScalaMethodChainInlaySettingsPanel.form)
final class ScalaMethodChainInlaySettingsPanel(separateColumnGetter: () => Boolean, separateColumnSetter: Boolean => Unit,
                                               typeCountGetter: () => Int, typeCountSetter: Int => Unit) {

  private var components: Option[(CheckBox, IntBox)] = None

  @RequiredUIAccess
  lazy val getPanel: JComponent = {
    val separateColumnCheckbox = CheckBox.create(LocalizeValue.of(ScalaCodeInsightBundle.message("in.a.separate.column")))
    separateColumnCheckbox.setValue(separateColumnGetter())
    separateColumnCheckbox.addValueListener(e => separateColumnSetter(e.getValue))

    val typeCountBox = IntBox.create(typeCountGetter())
    typeCountBox.setRange(1, 99)
    typeCountBox.addValueListener(e => typeCountSetter(e.getValue))

    components = Some((separateColumnCheckbox, typeCountBox))

    val typeCountLine = HorizontalLayout.create()
    typeCountLine.add(Label.create(LocalizeValue.of(ScalaCodeInsightBundle.message("inlay.hints.only.when.there.are.at.least"))))
    typeCountLine.add(typeCountBox)

    val panel = VerticalLayout.create()
    panel.add(separateColumnCheckbox)
    panel.add(typeCountLine)
    TargetAWT.to(panel).asInstanceOf[JComponent]
  }

  def reset(): Unit = components.foreach { case (separateColumnCheckbox, typeCountBox) =>
    separateColumnCheckbox.setValue(separateColumnGetter())
    typeCountBox.setValue(typeCountGetter())
  }
}
