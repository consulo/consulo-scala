package org.jetbrains.plugins.scala.worksheet.actions.repl

import consulo.annotation.component.ExtensionImpl

import consulo.ui.ex.action.AnAction
import consulo.dataContext.DataContext
import org.jetbrains.plugins.scala.actions.SingleActionPromoterBase

@ExtensionImpl
final class WorksheetReplRunActionPromoter extends SingleActionPromoterBase {

  override def shouldPromote(anAction: AnAction, context: DataContext): Boolean =
    anAction.isInstanceOf[WorksheetReplRunAction]
}
