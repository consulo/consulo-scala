package org.jetbrains.plugins.scala.actions.implicitArguments

import consulo.ui.ex.action.{ActionManager, ActionPromoter, AnAction}
import consulo.dataContext.DataContext

import java.util
import scala.jdk.CollectionConverters._

class ShowImplicitArgumentsActionPromoter extends ActionPromoter {
  // upstream: matched `com.intellij.codeInsight.hint.actions.ShowExpressionTypeAction` by class (impl class in Consulo)
  private def isShowExpressionTypeAction(action: AnAction): Boolean =
    ActionManager.getInstance.getId(action) == "ExpressionTypeInfo"

  override def promote(actions: util.List[? <: AnAction], context: DataContext): util.List[AnAction] = {
    val filtered = actions.asScala.filter {
      case _: ShowImplicitArgumentsAction => true
      case action => isShowExpressionTypeAction(action)
    }
    if (filtered.nonEmpty)
      filtered.sortBy {
        case _: ShowImplicitArgumentsAction => 1
        case action if isShowExpressionTypeAction(action) => 2
        case _ => 0
      }.asJava.asInstanceOf[util.List[AnAction]]
    else null
  }
}
