package org.jetbrains.sbt.shell.action

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.application.dumb.DumbAware
import consulo.ui.ex.action.{AnSeparator, DefaultActionGroup}

@ActionImpl(
  id = "Scala.Sbt.CommandMenu",
  children = Array(
    new ActionRef(id = "RunContextGroup"),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(`type` = classOf[RunCommandAction]),
    new ActionRef(`type` = classOf[SbtHelpAction])
  )
)
final class SbtCommandMenuGroup extends DefaultActionGroup with DumbAware {
  setPopup(true)
}
