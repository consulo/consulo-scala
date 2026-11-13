package org.jetbrains.plugins.scala.codeInspection.shadow

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.codeInspection.quickfix.RenameElementQuickfix
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypeParametersOwner

@ExtensionImpl
class TypeParameterShadowInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.suspicious.shadowing.by.a.type.parameter"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case refPat: ScTypeParam => check(refPat, holder)
    case _ =>
  }
  
  private def isShadowing(refPat: ScTypeParam): Option[ScTypeParametersOwner] = {
    var parent: PsiElement = refPat.getParent
    val owner = refPat.owner
    while (parent != null) {
      parent match {
        case t: ScTypeParametersOwner if t != owner =>
          for (param <- t.typeParameters) {
            if (refPat.name == param.name && refPat.name != "_") return Some(t)
          }
        case _ =>
      }
      parent = parent.getParent
    }
    None
  }

  private def check(typeParam: ScTypeParam, holder: ProblemsHolder): Unit = {
    isShadowing(typeParam) match {
      case Some(_) =>
        //noinspection ScalaExtractStringToBundle,ReferencePassedToNls
        holder.registerProblem(typeParam.nameId, getDisplayName.get + ": " + typeParam.name, new RenameTypeParameterFix(typeParam))
      case _ =>
    }
  }
}

class RenameTypeParameterFix(tp: ScTypeParam) extends RenameElementQuickfix(tp, ScalaInspectionBundle.message("rename.variable.pattern"))