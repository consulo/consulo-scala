package org.jetbrains.plugins.scala.lang.completion.statistician

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionLocation, CompletionStatistician}
import consulo.language.editor.completion.lookup.LookupElement
import consulo.language.statistician.StatisticsInfo
import com.intellij.java.language.psi.{PsiClass, PsiMember}
import org.jetbrains.plugins.scala.lang.completion.ScalaTextLookupItem
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAlias
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScModifierListOwner
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

@ExtensionImpl
class ScalaCompletionStatistician extends CompletionStatistician {
  override def serialize(element: LookupElement, location: CompletionLocation): StatisticsInfo = {
    ScalaLookupItem.delegate(element) match {
      case item: ScalaLookupItem =>
        item.getPsiElement match {
          case _ if item.isLocalVariable || item.isNamedParameter => StatisticsInfo.EMPTY
          case withImplicit: ScModifierListOwner if withImplicit.hasModifierPropertyScala("implicit") =>
            StatisticsInfo.EMPTY
          case member: PsiMember => helper(member)
          case _ => StatisticsInfo.EMPTY
        }
      // return empty statistic when using  scala completion but ScalaLookupItem didn't use.
      // otherwise will be computed java statistic that may lead to ClassCastError
      case _: ScalaTextLookupItem => StatisticsInfo.EMPTY
      case _ if location.getCompletionParameters.getOriginalFile.isInstanceOf[ScalaFile] => StatisticsInfo.EMPTY
      case _ => null //don't impact on java Lookups, no statistics for scala keyword elements
    }
  }

  private def helper(member: PsiMember): StatisticsInfo = {
    val key = ScalaStatisticManager.memberKey(member).getOrElse(return StatisticsInfo.EMPTY)
    member match {
      case _: ScTypeAlias | _: ScTypeDefinition | _: PsiClass =>
        new StatisticsInfo("scalaMember", key)
      case _ =>
        member.getContainingClass match {
          case null => StatisticsInfo.EMPTY
          case containingClass =>
            val context = ScalaStatisticManager.memberKey(containingClass).getOrElse("")
            new StatisticsInfo("scalaMember#" + context, key)
        }
    }
  }
}
