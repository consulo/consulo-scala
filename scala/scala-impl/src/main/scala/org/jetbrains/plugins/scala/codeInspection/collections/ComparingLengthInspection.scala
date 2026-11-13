package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScIntegerLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel

import scala.collection.immutable.ArraySeq

@ExtensionImpl
final class ComparingLengthInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.comparing.length.to.lengthcompare"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.comparing"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(ComparingLengthInspection)
}

object ComparingLengthInspection extends SimplificationType {
  private val hintLengthCompare: String = ScalaInspectionBundle.message("replace.with.lengthCompare")
  private val hintSizeIs: String = ScalaInspectionBundle.message("replace.with.sizeIs")

  override def hint: String = null

  override def getSimplification(e: ScExpression): Option[Simplification] = Some(e).collect {
    // TODO infix notation?
    case `.sizeOrLength`(q) `>` n => (q, ">", n)
    case `.sizeOrLength`(q) `>=` n => (q, ">=", n)
    case `.sizeOrLength`(q) `==` n => (q, "==", n)
    case `.sizeOrLength`(q) `!=` n => (q, "!=", n)
    case `.sizeOrLength`(q) `<` n => (q, "<", n)
    case `.sizeOrLength`(q) `<=` n => (q, "<=", n)
  }.filter { case (q, _, n) =>
    isNonIndexedSeq(q) && !isZero(n)
  }.map { case (q, op, n) =>
    val repl = replace(e).highlightFrom(q)
    if (doesScalaHasSizeIs(e)) repl.withText(s"${invocationText(q, "sizeIs")} $op ${n.getText}").withHint(hintSizeIs)
    else repl.withText(s"${invocationText(q, "lengthCompare", n)} $op 0").withHint(hintLengthCompare)
  }

  private def isZero(expression: ScExpression): Boolean = expression match {
    case ScIntegerLiteral(0) => true
    case _ => false
  }

  private def doesScalaHasSizeIs(e: ScExpression): Boolean =
    e.scalaLanguageLevel
      .forall(_ >= ScalaLanguageLevel.Scala_2_13)
}
