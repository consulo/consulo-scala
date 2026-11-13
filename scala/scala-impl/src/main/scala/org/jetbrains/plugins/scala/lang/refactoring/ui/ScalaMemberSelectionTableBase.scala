package org.jetbrains.plugins.scala.lang.refactoring.ui

import consulo.application.AllIcons
import consulo.language.psi.PsiElement
import com.intellij.java.language.psi.{PsiModifierList, PsiModifierListOwner}
import consulo.language.editor.refactoring.classMember.MemberInfoModel
import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable
import consulo.desktop.awt.ui.impl.image.RowIcon
import com.intellij.util.IconUtil
import com.intellij.java.language.util.VisibilityIcons
import org.jetbrains.plugins.scala.extensions.PsiModifierListOwnerExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScObject}

import javax.swing.Icon

abstract class ScalaMemberSelectionTableBase[M <: PsiElement, I <: ScalaMemberInfoBase[M]](memberInfos: java.util.Collection[I],
                                memberInfoModel: MemberInfoModel[M, I],
                                abstractColumnHeader: String)
        extends AbstractMemberSelectionTable[M, I](memberInfos, memberInfoModel, abstractColumnHeader) {

  override def getAbstractColumnValue(memberInfo: I): AnyRef = memberInfo.getMember match {
    case member: ScMember if member.containingClass.isInstanceOf[ScObject] => null
    case member: ScMember if member.hasAbstractModifier && myMemberInfoModel.isFixedAbstract(memberInfo) != null =>
      myMemberInfoModel.isFixedAbstract(memberInfo)
    case _ if !myMemberInfoModel.isAbstractEnabled(memberInfo) =>
      val res: java.lang.Boolean = myMemberInfoModel.isAbstractWhenDisabled(memberInfo)
      res
    case _ if memberInfo.isToAbstract => java.lang.Boolean.TRUE
    case _ => java.lang.Boolean.FALSE
  }

  override def isAbstractColumnEditable(rowIndex: Int): Boolean = {
    val info: I = myMemberInfos.get(rowIndex)
    info.getMember match {
      case member: ScMember if member.hasAbstractModifier && myMemberInfoModel.isFixedAbstract(info) == java.lang.Boolean.TRUE => false
      case _ => info.isChecked && myMemberInfoModel.isAbstractEnabled(info)
    }
  }

  override def setVisibilityIcon(memberInfo: I, icon: RowIcon): Unit = {
    memberInfo.getMember match {
      case owner: PsiModifierListOwner =>
        owner.getModifierList match {
          case mods: PsiModifierList => VisibilityIcons.setVisibilityIcon(mods, icon.asInstanceOf[com.intellij.ui.icons.RowIcon])
          case null => icon.setIcon(IconUtil.getEmptyIcon(true), AbstractMemberSelectionTable.VISIBILITY_ICON_POSITION)
        }
      case _ =>
    }
  }

  override def getOverrideIcon(memberInfo: I): Icon = memberInfo.getMember match {
    case _: ScFunction =>
      if (java.lang.Boolean.TRUE == memberInfo.getOverrides) AllIcons.General.OverridingMethod
      else if (java.lang.Boolean.FALSE == memberInfo.getOverrides) AllIcons.General.ImplementingMethod
      else AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON
    case _ => AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON
  }
}
