package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class OptionEqualsSomeToContainsInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.equals.some.x.to.contains.x"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.options"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "OptionEqualsSome"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] =
    ArraySeq(OptionEqualsSomeToContains, OptionNotEqualsSomeToNotContains)
}

object OptionEqualsSomeToContains extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.with.contains")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case _ if expr.scalaLanguageLevel.exists(_ <= ScalaLanguageLevel.Scala_2_10) => None
    case qual `==` `scalaSome`(elem) if isOption(qual) =>
      Some(replace(expr).withText(s"${qual.getText}.contains(${elem.getText})").highlightAll)
    case `scalaSome`(elem) `==` qual if isOption(qual) =>
      Some(replace(expr).withText(s"${qual.getText}.contains(${elem.getText})").highlightAll)
    case _ => None
  }
}

object OptionNotEqualsSomeToNotContains extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.with.not.contains")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case _ if expr.scalaLanguageLevel.exists(_ <= ScalaLanguageLevel.Scala_2_10) => None
    case qual `!=` `scalaSome`(elem) if isOption(qual) =>
      Some(replace(expr).withText(s"!${qual.getText}.contains(${elem.getText})").highlightAll)
    case `scalaSome`(elem) `!=` qual if isOption(qual) =>
      Some(replace(expr).withText(s"!${qual.getText}.contains(${elem.getText})").highlightAll)
    case _ => None
  }
}
