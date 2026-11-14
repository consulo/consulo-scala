package org.jetbrains.plugins.scala.lang.refactoring.changeSignature

import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.HorizontalLayout
import consulo.ui.{Label, RadioButton, RadioGroup}
import org.jetbrains.plugins.scala.ScalaBundle

class DefaultValuesUsagePanel(labelText: String = ScalaBundle.message("default.values")) {
  private val group: RadioGroup[java.lang.Boolean] = RadioGroup.create()
  private val myRbModifyCalls: RadioButton = group.newButton(LocalizeValue.of(ScalaBundle.message("modify.method.calls")), java.lang.Boolean.FALSE)
  private val myRbAddDefaultArg: RadioButton = group.newButton(LocalizeValue.of(ScalaBundle.message("add.to.definition")), java.lang.Boolean.TRUE)

  group.setValue(java.lang.Boolean.FALSE)

  val component: HorizontalLayout = {
    val layout = HorizontalLayout.create()
    if (labelText.nonEmpty) layout.add(Label.create(LocalizeValue.of(labelText)))
    layout.add(myRbModifyCalls)
    layout.add(myRbAddDefaultArg)
    layout
  }

  def isAddDefaultArgs: Boolean = myRbAddDefaultArg.getValueOrError

  @RequiredUIAccess
  def forceIsModifyCalls(): Unit = {
    group.setValue(java.lang.Boolean.FALSE)
    myRbModifyCalls.setEnabled(false)
    myRbAddDefaultArg.setEnabled(false)
  }

  @RequiredUIAccess
  def release(): Unit = {
    myRbModifyCalls.setEnabled(true)
    myRbAddDefaultArg.setEnabled(true)
  }
}
