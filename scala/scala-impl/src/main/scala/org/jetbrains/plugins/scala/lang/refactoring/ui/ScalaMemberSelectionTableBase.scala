package org.jetbrains.plugins.scala.lang.refactoring.ui

import consulo.platform.base.icon.PlatformIconGroup

import consulo.ui.image.Image
import consulo.language.psi.PsiElement
import consulo.language.editor.refactoring.classMember.MemberInfoModel
import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable
import org.jetbrains.plugins.scala.extensions.PsiModifierListOwnerExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScObject}


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

  // upstream: setVisibilityIcon override (no visibility icon hook in Consulo AbstractMemberSelectionTable)

  override def getOverrideIcon(memberInfo: I): Image = memberInfo.getMember match {
    case _: ScFunction =>
      if (java.lang.Boolean.TRUE == memberInfo.getOverrides) PlatformIconGroup.gutterOverridingmethod()
      else if (java.lang.Boolean.FALSE == memberInfo.getOverrides) PlatformIconGroup.gutterImplementingmethod()
      else AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON
    case _ => AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON
  }
}
