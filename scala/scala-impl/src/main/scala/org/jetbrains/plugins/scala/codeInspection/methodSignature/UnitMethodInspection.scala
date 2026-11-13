package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalQuickFix, ProblemHighlightType}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.ui.CompilerInspectionOptions
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScFunctionDeclaration, ScFunctionDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker
import org.jetbrains.plugins.scala.codeInspection.methodSignature.quickfix._

sealed abstract class UnitMethodInspection extends AbstractMethodSignatureInspection with DumbAware {

  override protected def isApplicable(function: ScFunction): Boolean =
    function.hasUnitResultType
}

object UnitMethodInspection {

  import quickfix._

  sealed abstract class ProcedureSyntax extends UnitMethodInspection {
    override protected def highlightType(function: ScFunction): ProblemHighlightType =
      if (isScala3WithoutMigrationFlag(function)) ProblemHighlightType.GENERIC_ERROR
      else super.highlightType(function)

    private[methodSignature] def isScala3WithoutMigrationFlag(function: ScFunction): Boolean =
      function.isInScala3File &&
        !CompilerInspectionOptions.isInspectionAllowed(
          function,
          checkCompilerOption = true,
          compilerOptionName = "-source:3.0-migration"
        )
  }

}

@ExtensionImpl
final class UnitMethodInspectionParameterless extends UnitMethodInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.unit.parameterless"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "UnitMethodIsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import UnitMethodInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) &&
      function.isParameterless &&
      function.superMethods.isEmpty

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] =
    Some(new AddEmptyParentheses(function))
}

@ExtensionImpl
final class UnitMethodInspectionProcedureDefinition extends UnitMethodInspection.ProcedureSyntax {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.procedure.definition"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "ProcedureDefinition"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import UnitMethodInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) && function.is[ScFunctionDefinition] &&
      !function.hasAssign &&
      !function.isConstructor &&
      IntentionAvailabilityChecker.checkInspection(this, function)

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] = {
    val quickFix = new AbstractFixOnPsiElement(
      ScalaInspectionBundle.message("convert.to.function.syntax"),
      function.asInstanceOf[ScFunctionDefinition]
    ) with DumbAware {
      override protected def doApplyFix(function: ScFunctionDefinition)(implicit project: Project): Unit = {
        removeAssignment(function)
        removeTypeElement(function)
        addUnitTypeElement(function)
      }
    }

    Some(quickFix)
  }
}

@ExtensionImpl
final class UnitMethodInspectionProcedureDeclaration extends UnitMethodInspection.ProcedureSyntax {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.procedure.declaration"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "ProcedureDeclaration"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import UnitMethodInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    super.isApplicable(function) && function.is[ScFunctionDeclaration] &&
      !function.hasExplicitType

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] = {
    val quickFix = new AbstractFixOnPsiElement(
      ScalaInspectionBundle.message("convert.to.function.syntax"),
      function
    ) with DumbAware {
      override protected def doApplyFix(function: ScFunction)(implicit project: Project): Unit = {
        def addChildNode(element: PsiElement): Unit =
          function.getNode.addChild(element.getNode)

        import ScalaPsiElementFactory.{createColon, createTypeElementFromText, createWhitespace}
        addChildNode(createColon)
        addChildNode(createWhitespace)
        addChildNode(createTypeElementFromText("Unit", function))
      }
    }

    Some(quickFix)
  }
}
