package org.jetbrains.plugins.scala
package codeInsight
package generation
package ui

import consulo.project.Project
import consulo.language.editor.refactoring.classMember.AbstractMemberInfoModel
import consulo.ui.ex.awt.DialogWrapper
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.awt.BorderLayout
import javax.swing.{JCheckBox, JComponent}

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

    private val checkBox = new JCheckBox(ScalaCodeInsightBundle.message("generate.ui.toString.withNames"))

    add(checkBox, BorderLayout.SOUTH)

    def isSelected: Boolean = checkBox.isSelected

    //noinspection AccessorLikeMethodIsUnit
    def isSelected_=(isSelected: Boolean): Unit = {
      checkBox.setSelected(isSelected)
    }
  }
}
