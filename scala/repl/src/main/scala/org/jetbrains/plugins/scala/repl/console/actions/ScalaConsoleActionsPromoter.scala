package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ExtensionImpl
import com.intellij.codeInsight.lookup.impl.actions.ChooseItemAction
import consulo.ui.ex.action.{ActionPromoter, AnAction}
import consulo.language.editor.CommonDataKeys
import consulo.dataContext.DataContext
import org.jetbrains.plugins.scala.repl.console.ScalaConsoleInfo
import org.jetbrains.plugins.scala.extensions.ObjectExt

import java.util
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class ScalaConsoleActionsPromoter extends ActionPromoter {
  //noinspection ScalaRedundantCast
  override def promote(actions: util.List[? <: AnAction], context: DataContext): util.List[AnAction] = {
    val isScalaConsoleEditor = Option(context.getData(CommonDataKeys.EDITOR)).exists(ScalaConsoleInfo.isConsole)
    if (isScalaConsoleEditor) {
      actions.asScala
        .filter((action: AnAction) => action.is[
          ScalaConsoleExecuteAction,
          ScalaConsoleNewLineAction,
          ScalaConsoleCleanAction,
          ChooseItemAction
        ])
        .asJava
        .asInstanceOf[util.List[AnAction]]
    } else {
      null
    }
  }
}