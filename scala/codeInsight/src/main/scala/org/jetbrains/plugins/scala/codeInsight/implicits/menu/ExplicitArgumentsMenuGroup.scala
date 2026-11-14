package org.jetbrains.plugins.scala.codeInsight.implicits.menu

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.ui.ex.action.{AnSeparator, DefaultActionGroup}
import org.jetbrains.plugins.scala.codeInsight.implicits.{ExpandImplicitHintsAction, ShowImplicitHintsAction}

@ActionImpl(
  id = "ExplicitArgumentsMenu",
  children = Array(
    new ActionRef(`type` = classOf[RemoveExplicitArguments]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(`type` = classOf[ShowImplicitHintsAction]),
    new ActionRef(`type` = classOf[ExpandImplicitHintsAction]),
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(id = "Scala.XRayModeTip"),
  )
)
final class ExplicitArgumentsMenuGroup extends DefaultActionGroup
