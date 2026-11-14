package org.jetbrains.plugins.scala.codeInsight.generation.actions

import consulo.annotation.component.{ActionImpl, ActionParentRef, ActionRef}
import consulo.ui.ex.action.{AnSeparator, DefaultActionGroup, IdeActions}

@ActionImpl(
  id = "ScalaGenerateGroup",
  children = Array(
    new ActionRef(`type` = classOf[AnSeparator]),
    new ActionRef(`type` = classOf[ScalaGenerateEqualsAction]),
    new ActionRef(`type` = classOf[ScalaGenerateToStringAction]),
    new ActionRef(`type` = classOf[ScalaGenerateCompanionObjectAction]),
    new ActionRef(`type` = classOf[ScalaGeneratePropertyAction]),
  ),
  parents = Array(new ActionParentRef(value = new ActionRef(id = IdeActions.GROUP_GENERATE)))
)
final class ScalaGenerateGroup extends DefaultActionGroup
