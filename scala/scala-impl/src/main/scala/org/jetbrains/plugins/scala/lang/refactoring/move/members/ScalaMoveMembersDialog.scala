package org.jetbrains.plugins.scala.lang.refactoring.move.members

import consulo.project.Project
import consulo.ui.ex.awt.Messages
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.PsiDocumentManager
import com.intellij.java.language.psi.PsiMember
import consulo.language.editor.refactoring.RefactoringBundle
import com.intellij.java.impl.refactoring.move.moveMembers.{MoveMembersOptions, MoveMembersProcessor}
import consulo.language.editor.refactoring.ui.RefactoringDialog
import consulo.language.editor.ui.awt.EditorComboBox
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScObject}
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment
import org.jetbrains.plugins.scala.api.{NlsString, ScalaFileType}
import org.jetbrains.plugins.scala.ScalaBundle

import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.VerticalLayout
import consulo.ui.{Label, TextBox}

import javax.swing.JComponent

class ScalaMoveMembersDialog(project: Project, canBeParent: Boolean, sourceObject: ScObject, memberToMove: ScMember) extends RefactoringDialog(project, canBeParent) {

  private val targetObjectFragment = ScalaCodeFragment("", memberToMove.getContext, memberToMove)(using project)

  private val myTfTargetClassName: EditorComboBox = {
    val document = PsiDocumentManager.getInstance(project).getDocument(targetObjectFragment)
    new EditorComboBox(document, project, ScalaFileType.INSTANCE)
  }

  init()

  override def doAction(): Unit = {
    val reference =
      PsiTreeUtil.getChildOfType(targetObjectFragment, classOf[ScReference])
        .toOption.toRight(NlsString(ScalaBundle.message("move.members.object.name.or.qualified.name.expected")))

    val maybeObj = reference.flatMap(
      _.resolve().asOptionOf[ScObject]
        .toRight(NlsString(ScalaBundle.message("move.members.cannot.find.object")))
    )

    maybeObj match {
      case Right(obj) =>
        val processor = ScalaMoveMembersDialog.createProcessor(obj, memberToMove)
        invokeRefactoring(processor)
      case Left(message) =>
        Messages.showErrorDialog(message.nls, RefactoringBundle.message("error.title"))
    }
  }

  @RequiredUIAccess
  override def createCenterPanel(): JComponent = {
    val sourceClassField = TextBox.create(sourceObject.name)
    sourceClassField.setEditable(false)

    val panel = VerticalLayout.create()
      .add(Label.create(LocalizeValue.of(ScalaBundle.message("move.members.source.title"))))
      .add(sourceClassField)
      .add(Label.create(LocalizeValue.of(ScalaBundle.message("move.members.target.title"))))
      .add(TargetAWT.wrap(myTfTargetClassName))

    validateButtons()
    TargetAWT.to(panel).asInstanceOf[JComponent]
  }

  override def getPreferredFocusedComponent: JComponent = myTfTargetClassName

}

object ScalaMoveMembersDialog {
  def createProcessor(obj: ScObject, member: ScMember): MoveMembersProcessor = {
    //noinspection ScalaWrongMethodsUsage
    val qualName = obj.getQualifiedName
    new MoveMembersProcessor(obj.getProject, null, new MoveMembersOptions() {
      override def getMemberVisibility: String = "public"

      override def makeEnumConstant: Boolean = false

      override def getSelectedMembers: Array[PsiMember] = Array(member)

      override def getTargetClassName: String = qualName
    })
  }
}