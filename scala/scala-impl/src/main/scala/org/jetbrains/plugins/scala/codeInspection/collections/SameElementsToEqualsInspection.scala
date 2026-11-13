package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class SameElementsToEqualsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.sameelements.oncollections.of.a.same.kind"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.comparing"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(SameElementsToEquals, CorrespondsToEquals)
}

object SameElementsToEquals extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.sameElements.with.equals")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case left`.sameElements`(right) if isOfSameKind(left, right) && !bothSortedSetsOrMaps(left, right) =>
      Some(replace(expr).withText(s"${left.getText} == ${right.getText}").highlightRef)
    case _ => None
  }

  private def isOfSameKind(left: ScExpression, right: ScExpression) = {
    val leftWithoutConversions = withoutConversions(left)
    val rightWithoutConversions = withoutConversions(right)
    isSet(leftWithoutConversions) && isSet(rightWithoutConversions) ||
            isSeq(leftWithoutConversions) && isSeq(rightWithoutConversions) ||
            isMap(leftWithoutConversions) && isMap(rightWithoutConversions)
  }

  private def bothSortedSetsOrMaps(left: ScExpression, right: ScExpression) = {
    isSortedSet(left) && isSortedSet(right) || isSortedMap(left) && isSortedMap(right)
  }
}

object CorrespondsToEquals extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.corresponds.with.equals")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case `.corresponds`(left, right, binaryOperation("==")) if isSeq(left) && isSeq(right) =>
      Some(replace(expr).withText(s"${left.getText} == ${right.getText}").highlightRef)
    case _ => None
  }
}

