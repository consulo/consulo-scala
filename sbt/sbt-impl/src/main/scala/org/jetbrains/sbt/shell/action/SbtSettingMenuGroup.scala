package org.jetbrains.sbt.shell.action

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.application.dumb.DumbAware
import consulo.ui.ex.action.{AnSeparator, DefaultActionGroup}

@ActionImpl(
  id = "Scala.Sbt.SettingMenu",
  children = Array(
    new ActionRef(id = "RunContextGroup"),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(`type` = classOf[ShowSettingAction]),
    new ActionRef(`type` = classOf[InspectSettingAction]),
    new ActionRef(`type` = classOf[SbtHelpAction])
  )
)
final class SbtSettingMenuGroup extends DefaultActionGroup with DumbAware {
  setPopup(true)
}
