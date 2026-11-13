package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.collections.UnzipSingleElementInspection._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class UnzipSingleElementInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.unzip.for.extracting.a.single.element"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.other"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(UnzipSingleElement)
}

private object UnzipSingleElementInspection {
  private val UnzipSingleElement: SimplificationType = new SimplificationType {

    override def hint: String = ScalaInspectionBundle.message("replace.with.map")

    override def getSimplification(e: ScExpression): Option[Simplification] = Some(e).collect {
      // TODO infix notation?
      case `._1`(`.unzip`(q)) => (q, 1)
      case `._2`(`.unzip`(q)) => (q, 2)
      case `._1`(`.unzip3`(q)) => (q, 1)
      case `._2`(`.unzip3`(q)) => (q, 2)
      case `._3`(`.unzip3`(q)) => (q, 3)
    }.map { case (q, index) =>
      val args = createExpressionFromText(s"_._$index", e)(using q.getContext)
      val text = invocationText(q, "map", args)
      replace(e).withText(text).highlightFrom(q)
    }

    private val `._1` = invocation("_1").from(ArraySeq("scala.Tuple2", "scala.Tuple3"))
    private val `._2` = invocation("_2").from(ArraySeq("scala.Tuple2", "scala.Tuple3"))
    private val `._3` = invocation("_3").from(ArraySeq("scala.Tuple3"))
  }

}
