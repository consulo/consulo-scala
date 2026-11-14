package org.jetbrains.plugins.scala.codeInsight.generation.ui

import consulo.language.editor.localize.CodeInsightLocalize
import consulo.language.editor.refactoring.classMember.AbstractMemberInfoModel
import consulo.project.Project
import consulo.ui.ex.awt.DialogWrapper
import org.jetbrains.plugins.scala.codeInsight.generation.{fields, isVar}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

import java.awt.GridLayout
import java.util
import javax.swing.event.TableModelEvent
import javax.swing.{JComponent, JPanel}
import scala.jdk.CollectionConverters._

final class ScalaGenerateEqualsWizard(clazz: ScClass, needEquals: Boolean, needHashCode: Boolean)
                                     (implicit project: Project)
  extends DialogWrapper(project) {

  import ScalaGenerateEqualsWizard._

  private val classFields: util.List[ScalaMemberInfo] =
    extractFields(!isVar(_)).map(_._1).asJava

  private val fieldsToHashCode: util.HashMap[ScNamedElement, ScalaMemberInfo] =
    if (needEquals && needHashCode) {
      val result = new util.HashMap[ScNamedElement, ScalaMemberInfo]()
      for {
        (info, member) <- extractFields(Function.const(true))
      } result.put(member, info)
      result
    } else null

  private val equalsPanel: ScalaMemberSelectionPanel =
    if (needEquals) new ScalaMemberSelectionPanel(CodeInsightLocalize.generateEqualsHashcodeEqualsFieldsChooserTitle().get, classFields)(ScalaEqualsMemberInfoModel)
    else null

  private val hashCodePanel: ScalaMemberSelectionPanel =
    if (needHashCode) {
      val infos = if (needEquals) updateInfos(classFields) else classFields
      new ScalaMemberSelectionPanel(CodeInsightLocalize.generateEqualsHashcodeHashcodeFieldsChooserTitle().get, infos)(ScalaHashCodeMemberInfoModel)
    }
    else null

  setTitle(CodeInsightLocalize.generateEqualsHashcodeWizardTitle())

  if (equalsPanel != null && hashCodePanel != null) {
    equalsPanel.getTable.getModel.addTableModelListener { (_: TableModelEvent) =>
      hashCodePanel.getTable.setMemberInfos(updateInfos(equalsPanel.getTable.getSelectedMemberInfos))
    }
  }

  init()

  def equalsFields: Iterable[ScNamedElement] = selectedFields(equalsPanel)

  def hashCodeFields: Iterable[ScNamedElement] = selectedFields(hashCodePanel)

  override protected def createCenterPanel(): JComponent = {
    val panels = Seq(equalsPanel, hashCodePanel).filter(_ != null)
    val result = new JPanel(new GridLayout(1, panels.size, 8, 0))
    panels.foreach(result.add)
    result
  }

  override def getPreferredFocusedComponent: JComponent =
    Option(equalsPanel).orElse(Option(hashCodePanel)).map(_.getTable).orNull

  private def extractFields(visibility: ScNamedElement => Boolean) =
    for {
      field <- fields(clazz)
      info = new ScalaMemberInfo(field)
      member = info.getMember
    } yield {
      info.setChecked(visibility(member))
      (info, member)
    }

  private def updateInfos(infos: util.Collection[? <: ScalaMemberInfo]): util.List[ScalaMemberInfo] =
    infos.asScala.toList.map { info =>
      fieldsToHashCode.get(info.getMember)
    }.asJava
}

object ScalaGenerateEqualsWizard {

  private object ScalaEqualsMemberInfoModel extends AbstractMemberInfoModel[ScNamedElement, ScalaMemberInfo]

  private object ScalaHashCodeMemberInfoModel extends AbstractMemberInfoModel[ScNamedElement, ScalaMemberInfo]

  private def selectedFields(panel: ScalaMemberSelectionPanel): Iterable[ScNamedElement] =
    if (panel == null) Iterable.empty else panel.members
}
