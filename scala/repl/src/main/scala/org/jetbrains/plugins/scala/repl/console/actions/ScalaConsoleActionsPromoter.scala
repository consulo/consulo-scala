package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.Editor
import consulo.ui.ex.action.{ActionManager, ActionPromoter, AnAction, IdeActions}
import consulo.dataContext.DataContext
import org.jetbrains.plugins.scala.repl.console.ScalaConsoleInfo
import org.jetbrains.plugins.scala.extensions.ObjectExt

import java.util
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class ScalaConsoleActionsPromoter extends ActionPromoter {
  override def promote(actions: util.List[AnAction], context: DataContext): util.List[AnAction] = {
    val isScalaConsoleEditor = Option(context.getData(Editor.KEY)).exists(ScalaConsoleInfo.isConsole)
    if (isScalaConsoleEditor) {
      actions.asScala
        .filter((action: AnAction) => action.is[
          ScalaConsoleExecuteAction,
          ScalaConsoleNewLineAction,
          ScalaConsoleCleanAction
        ] || isChooseLookupItemAction(action))
        .asJava
    } else {
      null
    }
  }

  // upstream: `action.is[ChooseItemAction]`, the class is internal in Consulo
  private def isChooseLookupItemAction(action: AnAction): Boolean =
    ScalaConsoleActionsPromoter.ChooseLookupItemActionIds.contains(ActionManager.getInstance.getId(action))
}

object ScalaConsoleActionsPromoter {
  private val ChooseLookupItemActionIds = Set(
    IdeActions.ACTION_CHOOSE_LOOKUP_ITEM,
    IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE,
    IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_COMPLETE_STATEMENT,
    IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_DOT
  )
}
