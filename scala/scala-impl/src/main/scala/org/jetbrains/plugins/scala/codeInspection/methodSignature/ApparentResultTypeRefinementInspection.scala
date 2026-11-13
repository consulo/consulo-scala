package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.LocalQuickFix
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScCompoundTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScFunctionDeclaration}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

@ExtensionImpl
final class ApparentResultTypeRefinementInspection extends AbstractMethodSignatureInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.result.type.refinement"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import ApparentResultTypeRefinementInspection._

  override protected def isApplicable(function: ScFunction): Boolean =
    function.isInstanceOf[ScFunctionDeclaration] && typeComponents(function).isDefined

  override protected def findProblemElement(function: ScFunction): Option[PsiElement] =
    function.returnTypeElement

  override protected def createQuickFix(function: ScFunction): Option[LocalQuickFix] = {
    val quickFix = new AbstractFixOnPsiElement(
      ScalaInspectionBundle.message("insert.missing.assignment"),
      function
    ) {
      override protected def doApplyFix(function: ScFunction)(implicit project: Project): Unit = for {
        (lastType, refinement) <- typeComponents(function)
        endIndex = lastType.getTextRange.getEndOffset - function.getTextRange.getStartOffset

        text = s"${function.getText.substring(0, endIndex)} = ${refinement.getText}"
        method = ScalaPsiElementFactory.createMethodFromText(text, function)
      } function.replace(method)
    }

    Some(quickFix)
  }
}

object ApparentResultTypeRefinementInspection {

  private def typeComponents(function: ScFunction) = for {
    case ScCompoundTypeElement(types, Some(refinement)) <- function.returnTypeElement
    lastType <- types.lastOption
  } yield (lastType, refinement)
}
