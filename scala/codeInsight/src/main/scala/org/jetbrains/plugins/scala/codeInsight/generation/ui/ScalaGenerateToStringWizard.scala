package org.jetbrains.plugins.scala
package codeInsight
package generation
package ui

import consulo.project.Project
import consulo.language.editor.refactoring.classMember.AbstractMemberInfoModel
import consulo.localize.LocalizeValue
import consulo.ui.CheckBox
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.DialogWrapper
import consulo.ui.ex.awtUnsafe.TargetAWT
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.awt.BorderLayout
import javax.swing.JComponent

/**
  * Dialog to select class members for generation of toString method.
  *
  * @param project      IntelliJ project.
  * @param classMembers Class members to choose from.
  */
// upstream: single-step AbstractWizard (not public API in Consulo)
final class ScalaGenerateToStringWizard(classMembers: Seq[ScNamedElement])
                                       (implicit project: Project)
  extends DialogWrapper(project) {

  import ScalaGenerateToStringWizard._

  private val toStringPanel = new Panel(classMembers)

  def membersWithSelection: (Seq[ScNamedElement], Boolean) =
    (toStringPanel.members.toSeq, toStringPanel.isSelected)

  setTitle(ScalaCodeInsightBundle.message("generate.ui.toString.title"))
  toStringPanel.isSelected = settings.isGenerateToStringWithPropertiesNames
  init()

  override protected def createCenterPanel(): JComponent = toStringPanel

  override def getPreferredFocusedComponent: JComponent = toStringPanel.getTable

  override protected def doOKAction(): Unit = {
    settings.setGenerateToStringWithPropertiesNames(toStringPanel.isSelected)
    super.doOKAction()
  }

  private def settings: ScalaProjectSettings = ScalaProjectSettings.getInstance(project)
}

object ScalaGenerateToStringWizard {

  private final class Panel(members: Seq[ScNamedElement])
    extends ScalaMemberSelectionPanel(
      ScalaCodeInsightBundle.message("generate.ui.toString.properties"),
      members,
      new AbstractMemberInfoModel[ScNamedElement, ScalaMemberInfo] {}
    ) {

    private val checkBox = CheckBox.create(LocalizeValue.of(ScalaCodeInsightBundle.message("generate.ui.toString.withNames")))

    add(TargetAWT.to(checkBox), BorderLayout.SOUTH)

    @RequiredUIAccess
    def isSelected: Boolean = checkBox.getValueOrError

    //noinspection AccessorLikeMethodIsUnit
    @RequiredUIAccess
    def isSelected_=(isSelected: Boolean): Unit = {
      checkBox.setValue(isSelected)
    }
  }
}
