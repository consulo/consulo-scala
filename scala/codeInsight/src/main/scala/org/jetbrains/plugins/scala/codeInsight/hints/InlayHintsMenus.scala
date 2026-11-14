package org.jetbrains.plugins.scala
package codeInsight
package hints

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.ex.action.{AnAction, AnActionEvent, AnSeparator, DefaultActionGroup}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.annotator.TypeMismatchHints
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

// upstream: the inlay context menus were anonymous ActionGroup instances (MenuProvider(actionGroup));
// Consulo renderers only support group ids, so every menu is a registered group.
object InlayHintsMenus {
  final val LocalVariableType = "Scala.LocalVariableTypeHintsMenu"
  final val MemberVariableType = "Scala.MemberVariableTypeHintsMenu"
  final val MethodResultType = "Scala.MethodResultTypeHintsMenu"
  final val MethodChain = "Scala.MethodChainHintsMenu"
  final val RangeForToAndUntil = "Scala.RangeHintsForToAndUntilMenu"
  final val ExclusiveRange = "Scala.ExclusiveRangeHintsMenu"
  final val ParameterNames = "Scala.ParameterNameHintsMenu"
}

abstract class DisableInlayHintsAction(@Nls text: String, setSettings: ScalaCodeInsightSettings => Unit)
  extends AnAction(LocalizeValue.of(text)) {

  override def actionPerformed(e: AnActionEvent): Unit = {
    setSettings(ScalaCodeInsightSettings.getInstance())
    ImplicitHints.updateInAllEditors()
  }
}

abstract class DisableTypeHintsAction(@Nls text: String, setSettings: ScalaCodeInsightSettings => Unit)
  extends AnAction(LocalizeValue.of(text)) {

  override def actionPerformed(e: AnActionEvent): Unit = {
    setSettings(ScalaCodeInsightSettings.getInstance())
    ScalaTypeHintsConfigurable.forceHintsUpdateOnNextPass()
    TypeMismatchHints.refreshIn(e.getData(Project.KEY))
  }
}

@ActionImpl(id = "Scala.DisableLocalVariableTypeHints")
final class DisableLocalVariableTypeHintsAction extends DisableTypeHintsAction(
  ScalaCodeInsightBundle.message("disable.type.hints.for.local.variables"),
  _.showLocalVariableType = false
)

@ActionImpl(id = "Scala.DisableMemberVariableTypeHints")
final class DisableMemberVariableTypeHintsAction extends DisableTypeHintsAction(
  ScalaCodeInsightBundle.message("disable.type.hints.for.member.variables"),
  _.showPropertyType = false
)

@ActionImpl(id = "Scala.DisableMethodResultTypeHints")
final class DisableMethodResultTypeHintsAction extends DisableTypeHintsAction(
  ScalaCodeInsightBundle.message("disable.type.hints.for.method.results"),
  _.showFunctionReturnType = false
)

@ActionImpl(id = "Scala.DisableMethodChainHints")
final class DisableMethodChainHintsAction extends DisableInlayHintsAction(
  ScalaCodeInsightBundle.message("disable.hints.for.method.chains"),
  _.showMethodChainInlayHints = false
)

@ActionImpl(id = "Scala.DisableRangeHintsForToAndUntil")
final class DisableRangeHintsForToAndUntilAction extends DisableInlayHintsAction(
  ScalaCodeInsightBundle.message("disable.range.hints.for.to.and.until"),
  _.showRangeHintsForToAndUntil = false
)

@ActionImpl(id = "Scala.DisableExclusiveRangeHints")
final class DisableExclusiveRangeHintsAction extends DisableInlayHintsAction(
  ScalaCodeInsightBundle.message("disable.hints.for.exclusive.ranges"),
  _.showExclusiveRangeHint = false
)

@ActionImpl(id = "Scala.DisableParameterNameHints")
final class DisableParameterNameHintsAction extends DisableInlayHintsAction(
  ScalaCodeInsightBundle.message("disable.hints.for.parameter.names"),
  _.showParameterNames = false
)

abstract class ConfigureInlayHintsAction(@Nls text: String, navigateTo: Project => Unit) extends AnAction(LocalizeValue.of(text)) {
  override def actionPerformed(e: AnActionEvent): Unit =
    navigateTo(e.getData(Project.KEY))
}

@ActionImpl(id = "Scala.ConfigureTypeHints")
final class ConfigureTypeHintsAction extends ConfigureInlayHintsAction(ScalaCodeInsightBundle.message("configure.type.hints"),
  ScalaTypeHintsSettingsModel.navigateTo)

@ActionImpl(id = "Scala.ConfigureParameterNameHints")
final class ConfigureParameterNameHintsAction extends ConfigureInlayHintsAction(ScalaCodeInsightBundle.message("configure.parameter.name.hints"),
  ScalaParameterHintsSettingsModel.navigateTo)

@ActionImpl(id = "Scala.ConfigureMethodChainHints")
final class ConfigureMethodChainHintsAction extends ConfigureInlayHintsAction(ScalaCodeInsightBundle.message("configure.method.chain.hints"),
  methodChains.ScalaMethodChainInlayHintsSettingsModel.navigateTo)

@ActionImpl(id = "Scala.ConfigureRangeHints")
final class ConfigureRangeHintsAction extends ConfigureInlayHintsAction(ScalaCodeInsightBundle.message("configure.range.hints"),
  rangeHints.RangeHintsForToAndUntilSettingsModel.navigateTo)

@ActionImpl(
  id = "Scala.LocalVariableTypeHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableLocalVariableTypeHintsAction]),
    new ActionRef(`type` = classOf[ConfigureTypeHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class LocalVariableTypeHintsMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.MemberVariableTypeHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableMemberVariableTypeHintsAction]),
    new ActionRef(`type` = classOf[ConfigureTypeHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class MemberVariableTypeHintsMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.MethodResultTypeHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableMethodResultTypeHintsAction]),
    new ActionRef(`type` = classOf[ConfigureTypeHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class MethodResultTypeHintsMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.MethodChainHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableMethodChainHintsAction]),
    new ActionRef(`type` = classOf[ConfigureMethodChainHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class MethodChainHintsMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.RangeHintsForToAndUntilMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableRangeHintsForToAndUntilAction]),
    new ActionRef(`type` = classOf[ConfigureRangeHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class RangeHintsForToAndUntilMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.ExclusiveRangeHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableExclusiveRangeHintsAction]),
    new ActionRef(`type` = classOf[ConfigureRangeHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class ExclusiveRangeHintsMenuGroup extends DefaultActionGroup

@ActionImpl(
  id = "Scala.ParameterNameHintsMenu",
  children = Array(
    new ActionRef(`type` = classOf[DisableParameterNameHintsAction]),
    new ActionRef(`type` = classOf[ConfigureParameterNameHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class ParameterNameHintsMenuGroup extends DefaultActionGroup
