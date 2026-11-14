package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.localize.LocalizeValue
import consulo.language.editor.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.annotator.{Bin, Hex, Oct}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral.Numeric
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.{ScIntegerLiteral, ScLongLiteral}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScType, ScTypeExt, api}

sealed abstract class NumberLiteralQuickFix[L <: Numeric](private val literal: L)
  extends IntentionAction
    with DumbAware {
  protected def transformText(text: String): String

  final def getFamilyName: String = ScalaBundle.message("numeric.literal.family")

  override final def isAvailable(project: Project,
                                 editor: Editor,
                                 file: PsiFile): Boolean =
    literal.isValid

  override final def invoke(project: Project,
                            editor: Editor,
                            file: PsiFile): Unit =
    if (literal.isValid) replaceLiteral(literal)

  override final def startInWriteAction: Boolean = true

  def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = {
    replaceLiteral(PsiTreeUtil.findSameElementInCopy(literal, file))
    IntentionPreviewInfo.DIFF
  }

  private def replaceLiteral(literal: L): Unit = {
    val newText = transformText(literal.getText)
    literal.replace {
      ScalaPsiElementFactory.createExpressionFromText(newText, literal)(using literal)
    }
  }
}

object NumberLiteralQuickFix {

  final class ConvertToLong(literal: ScIntegerLiteral) extends NumberLiteralQuickFix(literal) with SyntheticIntentionAction {

    override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("convert.to.long.fix"))

    override protected def transformText(text: String): String =
      ConvertToLong.transformText(text)
  }

  private[annotator] object ConvertToLong {

    val Marker = 'L'

    def isApplicableTo(literal: ScLiteral, expectedType: ScType): Boolean = {
      implicit val context: Context = Context(literal)

      val types = api.Long(using literal.getProject) ::
        ScalaPsiElementFactory.createTypeFromText(
          "_root_.scala.math.BigInt",
          literal.getContext,
          literal
        ).toList

      types.exists(_.weakConforms(expectedType))
    }

    def transformText(text: String): String = text + Marker
  }

  final class ConvertOctToHex(literal: Numeric,
                              isLong: Boolean) extends NumberLiteralQuickFix(literal) with SyntheticIntentionAction {

    override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("convert.octal.to.hex.fix"))

    override protected def transformText(text: String): String =
      Oct.to(Hex)(text, isLong) match {
        case result if isLong => ConvertToLong.transformText(result)
        case result => result
      }
  }

  final class ConvertBinaryToHex(literal: Numeric,
                                 isLong: Boolean) extends NumberLiteralQuickFix(literal) with SyntheticIntentionAction {

    override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("convert.binary.to.hex.fix"))

    override protected def transformText(text: String): String =
      Bin.to(Hex)(text, isLong) match {
        case result if isLong => ConvertToLong.transformText(result)
        case result => result
      }
  }

  final class ConvertMarker(literal: ScLongLiteral) extends NumberLiteralQuickFix(literal) with SyntheticIntentionAction {

    override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("lowercase.long.marker.fix"))

    override protected def transformText(text: String): String =
      ConvertToLong.transformText(text.dropRight(1))
  }

  object ConvertMarker {

    def isApplicableTo(literal: ScLongLiteral): Boolean =
      literal.getText.last.isLower
  }
}
