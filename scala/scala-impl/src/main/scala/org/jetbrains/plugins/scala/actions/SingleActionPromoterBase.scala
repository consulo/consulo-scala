package org.jetbrains.plugins.scala.actions

import consulo.ui.ex.action.{ActionPromoter, AnAction}
import consulo.dataContext.DataContext

import java.util
import java.util.Collections

abstract class SingleActionPromoterBase extends ActionPromoter {
  def shouldPromote(anAction: AnAction, context: DataContext): Boolean
  
  override def promote(actions: util.List[AnAction], context: DataContext): util.List[AnAction] = {
    val it = actions.iterator()

    while (it.hasNext) {
      val a = it.next()
      if (shouldPromote(a, context)) return util.Arrays.asList(a)
    }

    Collections.emptyList()
  }
}
