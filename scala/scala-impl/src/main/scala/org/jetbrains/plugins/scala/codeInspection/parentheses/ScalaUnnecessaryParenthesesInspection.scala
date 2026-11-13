package org.jetbrains.plugins.scala.codeInspection.parentheses

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.InspectionToolState
import consulo.localize.LocalizeValue
import consulo.ui.Label
import consulo.util.xml.serializer.XmlSerializerUtil
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import scala.beans.BooleanBeanProperty

@ExtensionImpl(id = "ScalaUnnecessaryParentheses")
class ScalaUnnecessaryParenthesesInspection extends ScalaUnnecessaryParenthesesInspectionBase {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.unnecessary.parentheses"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def createStateProvider(): InspectionToolState[?] = new ScalaUnnecessaryParenthesesInspectionState

  override protected def settingsOf(state: Object): UnnecessaryParenthesesSettings =
    state.asInstanceOf[ScalaUnnecessaryParenthesesInspectionState].settings
}

final class ScalaUnnecessaryParenthesesInspectionState extends InspectionToolState[ScalaUnnecessaryParenthesesInspectionState] {
  @BooleanBeanProperty var ignoreClarifying: Boolean = true
  @BooleanBeanProperty var ignoreAroundFunctionType: Boolean = false
  @BooleanBeanProperty var ignoreAroundFunctionTypeParam: Boolean = false
  @BooleanBeanProperty var ignoreAroundFunctionExprParam: Boolean = false

  def settings: UnnecessaryParenthesesSettings =
    UnnecessaryParenthesesSettings(ignoreClarifying, ignoreAroundFunctionType, ignoreAroundFunctionTypeParam, ignoreAroundFunctionExprParam)

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .component(() => Label.create(LocalizeValue.of(ScalaInspectionBundle.message("unnecessary.parentheses.ignore"))))
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("clarifying.parentheses")), () => ignoreClarifying, ignoreClarifying = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("around.function.type")), () => ignoreAroundFunctionType, ignoreAroundFunctionType = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("around.function.type.parameter")), () => ignoreAroundFunctionTypeParam, ignoreAroundFunctionTypeParam = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("around.function.expr.parameter")), () => ignoreAroundFunctionExprParam, ignoreAroundFunctionExprParam = _)
      .buildUnnamed()

  override def getState: ScalaUnnecessaryParenthesesInspectionState = this

  override def loadState(state: ScalaUnnecessaryParenthesesInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
