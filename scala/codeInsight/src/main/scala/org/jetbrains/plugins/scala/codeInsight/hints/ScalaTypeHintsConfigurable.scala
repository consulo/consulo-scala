package org.jetbrains.plugins.scala
package codeInsight
package hints

import consulo.annotation.component.ActionImpl
import consulo.ide.setting.ShowSettingsUtil
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.ex.action.{AnAction, AnActionEvent, AnActionWithSyncUpdate, ToggleAction}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings.{getInstance => ScalaCodeInsightSettings}
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.settings.sections.XRayModeSettingsSectionConfigurable

import java.util.function.{Consumer, Supplier}

object ScalaTypeHintsConfigurable {

  import java.lang.{Boolean => JBoolean}

  private[hints] def forceHintsUpdateOnNextPass(): Unit = {
    ImplicitHints.updateInAllEditors()
  }

  sealed abstract class ToggleTypeAction(@Nls text: String,
                                         @Nls description: String,
                                         getter: Supplier[JBoolean],
                                         setter: Consumer[JBoolean]) extends ToggleAction(LocalizeValue.of(text), LocalizeValue.of(description), null) {

    override def isSelected(event: AnActionEvent): Boolean = getter.get()

    override def setSelected(event: AnActionEvent, value: Boolean): Unit = {
      setter.accept(value)
      forceHintsUpdateOnNextPass()
    }
  }

  object XRayModeTipAction {
    final val Id = "Scala.XRayModeTip"
  }
}

import org.jetbrains.plugins.scala.codeInsight.hints.ScalaTypeHintsConfigurable.ToggleTypeAction

@ActionImpl(id = "Scala.ToggleMethodChainInlayHints")
class ScalaTypeHintsConfigurableToggleMethodChainInlayHintsAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("method.chain.hints.action.text"),
  ScalaCodeInsightBundle.message("method.chain.hints.action.description"),
  ScalaCodeInsightSettings.showMethodChainInlayHintsGetter(),
  ScalaCodeInsightSettings.showMethodChainInlayHintsSetter()
)

@ActionImpl(id = "Scala.ToggleRangeHintsForToAndUntilAction")
class ScalaTypeHintsConfigurableToggleRangeHintsForToAndUntilAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("range.hints.for.to.and.until.action.text"),
  ScalaCodeInsightBundle.message("show.range.hints.for.to.and.until"),
  ScalaCodeInsightSettings.showRangeHintsForToAndUntilGetter(),
  ScalaCodeInsightSettings.showRangeHintsForToAndUntilSetter()
)

@ActionImpl(id = "Scala.ToggleRangeExclusiveHintAction")
class ScalaTypeHintsConfigurableToggleRangeExclusiveHintAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("show.exclusive.range.hint.action.text"),
  ScalaCodeInsightBundle.message("show.exclusive.range.hint"),
  ScalaCodeInsightSettings.showExclusiveRangeHintDefaultGetter(),
  ScalaCodeInsightSettings.showExclusiveRangeHintDefaultSetter()
)

@ActionImpl(id = "Scala.ToggleTypeMismatchHints")
class ScalaTypeHintsConfigurableToggleTypeHintsAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("type.hints.action.text"),
  ScalaCodeInsightBundle.message("type.hints.action.description"),
  () => {
    ScalaCodeInsightSettings.showMethodResultTypeGetter().get() &&
      ScalaCodeInsightSettings.showMemberVariableTypeGetter().get() &&
      ScalaCodeInsightSettings.showLocalVariableTypeGetter().get()
  },
  (b) => {
    ScalaCodeInsightSettings.showMethodResultTypeSetter().accept(b)
    ScalaCodeInsightSettings.showMemberVariableSetter().accept(b)
    ScalaCodeInsightSettings.showLocalVariableTypeSetter().accept(b)
  },
)

/**
 * A no-op action to provide a tip.
 */
@ActionImpl(id = "Scala.XRayModeTip")
class ScalaTypeHintsConfigurableXRayModeTipAction extends AnAction with AnActionWithSyncUpdate {
  override def update(e: AnActionEvent): Unit = {
    e.getPresentation.setText(ScalaCodeInsightBundle.message("xray.mode.tip.context.menu", ScalaHintsSettings.xRayModeShortcut))
  }

  override def actionPerformed(e: AnActionEvent): Unit =
    ShowSettingsUtil.getInstance.showSettingsDialog(e.getData(Project.KEY), classOf[XRayModeSettingsSectionConfigurable])
}

@ActionImpl(id = "Scala.ToggleParametersHints")
class ScalaTypeHintsConfigurableToggleParameterHintsAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("parameter.name.hints.action.text"),
  ScalaCodeInsightBundle.message("parameter.name.hints.action.description"),
  ScalaCodeInsightSettings.showParameterNamesGetter(),
  ScalaCodeInsightSettings.showParameterNamesSetter()
)

@ActionImpl(id = "Scala.ToggleMethodResultTypeAction")
class ScalaTypeHintsConfigurableToggleMethodResultTypeAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("show.method.result.action.text"),
  ScalaCodeInsightBundle.message("show.method.result.action.description"),
  ScalaCodeInsightSettings.showMethodResultTypeGetter,
  ScalaCodeInsightSettings.showMethodResultTypeSetter,
)

@ActionImpl(id = "Scala.ToggleMemberVariableTypeAction")
class ScalaTypeHintsConfigurableToggleMemberVariableTypeAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("show.member.variable.action.text"),
  ScalaCodeInsightBundle.message("show.member.variable.action.description"),
  ScalaCodeInsightSettings.showMemberVariableTypeGetter,
  ScalaCodeInsightSettings.showMemberVariableSetter,
)

@ActionImpl(id = "Scala.ToggleLocalVariableTypeAction")
class ScalaTypeHintsConfigurableToggleLocalVariableTypeAction extends ToggleTypeAction(
  ScalaCodeInsightBundle.message("show.local.variable.action.text"),
  ScalaCodeInsightBundle.message("show.local.variable.action.description"),
  ScalaCodeInsightSettings.showLocalVariableTypeGetter,
  ScalaCodeInsightSettings.showLocalVariableTypeSetter,
)
