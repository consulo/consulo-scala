package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{ProblemHighlightType, ProblemsHolder}
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class TakeZeroInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.take.zero.qualifier.no.effect"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.simplifications.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "TakeZeroQualifierNoEffect"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case expr@_ `.take` arg if arg.textMatches("0") =>
      holder.registerProblem(expr, ScalaInspectionBundle.message("take.0.is.always.empty"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    case _ =>
  }

  override def possibleSimplificationTypes: Seq[SimplificationType] = ArraySeq.empty
}
