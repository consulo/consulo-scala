package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.LocalQuickFix
import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

sealed abstract class EmptyParenOverrideInspection extends AbstractMethodSignatureInspection {

  override protected def isApplicable(function: ScFunction): Boolean =
    function.isEmptyParen && function.superMethods.headOption.exists(isValidSuperMethod)

  protected def isValidSuperMethod(method: PsiMethod): Boolean

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] =
    Some(new quickfix.RemoveParentheses(function))
}

object EmptyParenOverrideInspection {

}

@ExtensionImpl
final class EmptyParenOverrideInspectionJavaAccessorMethodOverriddenAsEmptyParenInspection extends EmptyParenOverrideInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.empty.paren.override.java.accessor"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "JavaAccessorMethodOverriddenAsEmptyParen"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import EmptyParenOverrideInspection._

  override protected def isValidSuperMethod(method: PsiMethod): Boolean =
    quickfix.isAccessor(method)
}

@ExtensionImpl
final class EmptyParenOverrideInspectionParameterlessMemberOverriddenAsEmptyParenInspection extends EmptyParenOverrideInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.empty.paren.override.parameterless"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "ParameterlessMemberOverriddenAsEmptyParen"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import EmptyParenOverrideInspection._

  override protected def isValidSuperMethod(method: PsiMethod): Boolean = method match {
    case method: ScFunction if !method.isInCompiledFile => method.isParameterless
    case _ => false
  }
}
