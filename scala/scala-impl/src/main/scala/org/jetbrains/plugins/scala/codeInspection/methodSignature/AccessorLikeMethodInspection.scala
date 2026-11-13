package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.LocalQuickFix
import com.intellij.java.language.psi.PsiClass
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTrait

sealed abstract class AccessorLikeMethodInspection extends AbstractMethodSignatureInspection {

  override protected def isApplicable(function: ScFunction): Boolean =
    function.isValid &&
      function.hasQueryLikeName &&
      function.superMethods.isEmpty
}

object AccessorLikeMethodInspection {

  private[methodSignature] val JsAnyFqn = "scala.scalajs.js.Any"

  private[methodSignature] def isScalaJsFacade(clazz: PsiClass) =
    clazz.elementScope.getCachedClass(JsAnyFqn).exists {
      case scalaTrait: ScTrait => clazz.isInheritor(scalaTrait, true)
      case _ => false
    }
}

@ExtensionImpl
final class AccessorLikeMethodInspectionEmptyParentheses extends AccessorLikeMethodInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.accessor.empty.parenthesis"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "AccessorLikeMethodIsEmptyParen"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import AccessorLikeMethodInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) &&
      function.isEmptyParen &&
      !function.hasUnitResultType &&
      !Option(function.containingClass).exists(isScalaJsFacade)

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] =
    Some(new quickfix.RemoveParentheses(function))
}

@ExtensionImpl
final class AccessorLikeMethodInspectionUnitReturnType extends AccessorLikeMethodInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.accessor.unit.return.type"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "AccessorLikeMethodIsUnit"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import AccessorLikeMethodInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) && function.hasUnitResultType
}
