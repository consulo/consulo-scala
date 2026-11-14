package org.jetbrains.plugins.scala.testingSupport
package test.ui

import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.action.AnActionEvent
import consulo.ui.ex.toolbar.{AddAction, DownMoveAction, EditAction, RemoveAction, ToolbarDecoratorBuilderFactory, UpMoveAction}
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.{Component, Table, TableItemEditor, TextBox, ValueComponent}
import org.jetbrains.plugins.scala.testingSupport.test.ui.RegexpPanel._

import scala.jdk.CollectionConverters._

final class RegexpPanel @RequiredUIAccess() {

  private val myModel: MutableFlatDataModel[RegexpRow] = FlatDataModel.of(java.util.List.of[RegexpRow]())

  private val myRegexpTable: Table[RegexpRow] = {
    val table = Table.create(myModel)
    table.addColumn[String](LocalizeValue.of(TestingSupportBundle.message("test.run.config.for.class.pattern")), _.classPattern)
      .setEditor(new PatternEditor(_.classPattern, _.classPattern = _))
    table.addColumn[String](LocalizeValue.of(TestingSupportBundle.message("test.run.config.test.pattern")), _.testPattern)
      .setEditor(new PatternEditor(_.testPattern, _.testPattern = _))
    table
  }

  val getComponent: Component =
    ToolbarDecoratorBuilderFactory.getInstance()
      .create(myRegexpTable)
      .addOrReplaceAction(new AddAction[RegexpRow] {
        @RequiredUIAccess
        override protected def doAdd(e: AnActionEvent): Unit = {
          val selected = myRegexpTable.getSelectedItem
          val rowAdd = if (selected == null) myModel.getSize else myModel.indexOf(selected) + 1
          val row = new RegexpRow("", "")
          myModel.add(row, rowAdd)
          myRegexpTable.select(row)
        }
      })
      .addOrReplaceAction(new RemoveAction[RegexpRow] {
        @RequiredUIAccess
        override protected def doRemove(value: RegexpRow, e: AnActionEvent): Unit = {
          val row = myModel.indexOf(value)
          myModel.remove(value)
          if (row > 0)
            myRegexpTable.select(myModel.get(row - 1))
        }
      })
      .disableAction(classOf[EditAction[?]])
      .disableAction(classOf[UpMoveAction[?]])
      .disableAction(classOf[DownMoveAction[?]])
      .build()

  @RequiredUIAccess
  def setRegexps(classRegexps: Array[String], testRegexps: Array[String]): Unit = {
    val rows = classRegexps.zipAll(testRegexps, "", "").map { case (cr, tr) => new RegexpRow(cr, tr) }
    myModel.replaceAll(rows.toSeq.asJava)
  }

  def getRegexps: (Array[String], Array[String]) = {
    val rows = myModel.iterator().asScala.toArray
    (rows.map(_.classPattern), rows.map(_.testPattern))
  }

  private class PatternEditor(getter: RegexpRow => String, setter: (RegexpRow, String) => Unit) extends TableItemEditor[RegexpRow, String] {
    @RequiredUIAccess
    override def createComponent(item: RegexpRow): ValueComponent[String] = TextBox.create(getter(item))

    @RequiredUIAccess
    override def commit(item: RegexpRow, value: String): Unit = {
      setter(item, Option(value).getOrElse(""))
      myModel.update(item)
    }
  }
}

private object RegexpPanel {

  // identity of a row is the instance (FlatDataModel.getId)
  private final class RegexpRow(var classPattern: String, var testPattern: String)
}
