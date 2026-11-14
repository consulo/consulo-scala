package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.language.codeStyle.CodeStyleSettings
import consulo.localize.LocalizeValue
import consulo.ui.{CheckBox, Component}
import consulo.ui.layout.LabeledLayout
import org.jetbrains.plugins.scala.ScalaBundle

class Scala3SettingsPanel(settings: CodeStyleSettings) extends ScalaCodeStyleSubPanelBase(settings) {

  private val checkbox: CheckBox = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scala3.panel.use.indentation.based.syntax")))

  private val innerPanel: Component = LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("scala3.panel.title")), checkbox)

  override def apply(settings: CodeStyleSettings): Unit = {
    val ss = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    ss.USE_SCALA3_INDENTATION_BASED_SYNTAX = checkbox.getValue
  }

  override def isModified(settings: CodeStyleSettings): Boolean = {
    val ss = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    checkbox.getValue != ss.USE_SCALA3_INDENTATION_BASED_SYNTAX
  }

  override def getComponent: Component = innerPanel

  override def resetImpl(settings: CodeStyleSettings): Unit = {
    val ss = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    checkbox.setValue(ss.USE_SCALA3_INDENTATION_BASED_SYNTAX)
  }
}
