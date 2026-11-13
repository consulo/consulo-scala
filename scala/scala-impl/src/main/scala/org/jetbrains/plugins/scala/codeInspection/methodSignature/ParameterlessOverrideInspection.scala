package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.LocalQuickFix
import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScUnderScoreSectionUtil
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScFunctionDefinition}

sealed abstract class ParameterlessOverrideInspection extends AbstractMethodSignatureInspection {

  override protected def isApplicable(function: ScFunction): Boolean =
    function.isParameterless && superMethodsAreValid(function.superMethods)

  protected def superMethodsAreValid(methods: Iterable[PsiMethod]): Boolean

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] =
    Some(new quickfix.AddEmptyParentheses(function))
}

object ParameterlessOverrideInspection {

  import quickfix._

}

@ExtensionImpl
final class ParameterlessOverrideInspectionEmptyParenMethod extends ParameterlessOverrideInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.parameterless.override.empty.paren"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "EmptyParenMethodOverriddenAsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ParameterlessOverrideInspection._

  override protected def superMethodsAreValid(methods: Iterable[PsiMethod]): Boolean =
    methods.headOption.exists {
      case function: ScFunction if !function.isInCompiledFile => function.isEmptyParen
      case _ => false
    }
}

@ExtensionImpl
final class ParameterlessOverrideInspectionJavaMutator extends ParameterlessOverrideInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.parameterless.override.java.mutator"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "JavaMutatorMethodOverriddenAsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ParameterlessOverrideInspection._

  override protected def superMethodsAreValid(methods: Iterable[PsiMethod]): Boolean =
    methods.headOption.exists(isMutator)
}

@ExtensionImpl
final class ParameterlessOverrideInspectionMutatorLikeMethod extends ParameterlessOverrideInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.parameterless.override.mutator.like"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "MutatorLikeMethodIsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ParameterlessOverrideInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) &&
      !function.hasUnitResultType &&  // when the type is Unit, UnitMethodInspection#Parameterless is applicable
      hasMutatorLikeName(function) &&
      !isUnderscoreFunction(function)

  override protected def superMethodsAreValid(methods: Iterable[PsiMethod]): Boolean = methods.isEmpty

  private def isUnderscoreFunction(function: ScFunction): Boolean = function match {
    case ScFunctionDefinition.withBody(body) => ScUnderScoreSectionUtil.isUnderscoreFunction(body)
    case _ => false
  }
}
