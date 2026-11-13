package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt}
import org.jetbrains.plugins.scala.lang.psi.types.api.StdTypes

import scala.collection.immutable.ArraySeq

@ExtensionImpl
class IfElseToOptionInspection extends OperationOnCollectionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.emulated.option.x"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.options"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def possibleSimplificationTypes: ArraySeq[SimplificationType] = ArraySeq(IfElseToOption)
}

object IfElseToOption extends SimplificationType {
  override def hint: String = ScalaInspectionBundle.message("hint.replace.with.option.expr")

  override def getSimplification(expr: ScExpression): Option[Simplification] = {
    implicit val context: Context = Context(expr)

    val inner = expr match {
      case IfStmt(x `==` literal("null"), scalaNone(), scalaSome(x1))
        if areElementsEquivalent(x, x1) =>
        Some((x, x1))
      case IfStmt(x `!=` literal("null"), scalaSome(x1), scalaNone())
        if areElementsEquivalent(x, x1) =>
        Some((x, x1))
      case IfStmt(literal("null") `==` x, scalaNone(), scalaSome(x1))
        if areElementsEquivalent(x, x1) =>
        Some((x, x1))
      case IfStmt(literal("null") `!=` x, scalaSome(x1), scalaNone())
        if areElementsEquivalent(x, x1) =>
        Some((x, x1))
      case _ => None
    }
    val anyRef = StdTypes.instance(using expr).AnyRef
    inner.filterNot {
      case (in, out) =>
        // check if the value would be converted into a value type before being given into Option(...)
        in.`type`().exists(_.conforms(anyRef)) &&
          out.`type`().exists(!_.conforms(anyRef))
    }.map { case (x, _) =>
      val text = x.getText
      replace(expr)
        .withText(s"Option($text)")
        .withHint(ScalaInspectionBundle.message("hint.replace.with.option.expr.with.preview", text))
        .highlightAll
    }
  }
}
