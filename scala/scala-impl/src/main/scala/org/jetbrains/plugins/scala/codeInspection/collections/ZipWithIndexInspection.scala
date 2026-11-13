package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

import scala.collection.immutable.ArraySeq

object ZipWithIndexInspection extends SimplificationType() {
  override def hint: String = ScalaInspectionBundle.message("replace.with.zipWithIndex")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    expr match {
      case (ref @ ResolvesTo(x))`.zip`(`.indices`(ResolvesTo(y))) if x == y && !x.is[PsiMethod] =>
        Some(replace(expr).withText(invocationText(ref, "zipWithIndex")).highlightFrom(ref))
      case _ => None
    }
 }
}

@ExtensionImpl
class ZipWithIndexInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.manually.zipping.with.indices"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.indices"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(ZipWithIndexInspection)
}
