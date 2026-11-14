package org.jetbrains.sbt.shell.action

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.application.dumb.DumbAware
import consulo.ui.ex.action.{AnSeparator, DefaultActionGroup}

@ActionImpl(
  id = "Scala.Sbt.TaskMenu",
  children = Array(
    new ActionRef(id = "RunContextGroup"),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(`type` = classOf[RunTaskAction]),
    new ActionRef(`type` = classOf[ShowTaskAction]),
    new ActionRef(`type` = classOf[InspectTaskAction]),
    new ActionRef(`type` = classOf[SbtHelpAction])
  )
)
final class SbtTaskMenuGroup extends DefaultActionGroup with DumbAware {
  setPopup(true)
}
