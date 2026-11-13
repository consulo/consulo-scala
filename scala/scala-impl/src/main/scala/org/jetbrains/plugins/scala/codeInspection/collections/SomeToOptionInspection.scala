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
final class SomeToOptionInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.some.to.option"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.options"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(SomeToOptionInspection)
}

object SomeToOptionInspection extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("replace.with.option")

  override def getSimplification(expr: ScExpression): Option[Simplification] = expr match {
    case `scalaSome`(e) => Some(replace(expr).withText(s"Option(${e.getText})").highlightFrom(expr))
    case _ => None
  }
}
