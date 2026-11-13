package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.ProblemsHolder
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.&
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class CorrespondsUnsortedInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.corresponds.sameelements.on.unsorted.collection"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.comparing"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq.empty

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case (expr: ScExpression) & (left `.sameElements` right) if isUnsorted(left) || isUnsorted(right) =>
      holder.registerProblem(refNameId(expr).getOrElse(expr), ScalaInspectionBundle.message("sameElements.unsorted"))
    case (expr: ScExpression) & (`.corresponds`(left, right, _)) if isIterator(left) && isUnsorted(right) =>
    //corresponds signature imply that check is needed for iterators only
      holder.registerProblem(refNameId(expr).getOrElse(expr), ScalaInspectionBundle.message("corresponds.unsorted"))
    case _ =>
  }

  private def isUnsorted(expr: ScExpression): Boolean =
    !(isSeq(expr) || isSortedMap(expr) || isSortedSet(expr) || isArray(expr) || isIterator(expr))
}
