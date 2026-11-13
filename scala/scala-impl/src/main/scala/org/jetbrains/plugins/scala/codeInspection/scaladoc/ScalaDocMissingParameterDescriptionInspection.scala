package org.jetbrains.plugins.scala.codeInspection.scaladoc

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import consulo.language.ast.TokenSet
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.scaladoc.ScalaDocMissingParameterDescriptionInspection._
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag

@ExtensionImpl
final class ScalaDocMissingParameterDescriptionInspection extends LocalInspectionTool with DumbAware {
  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.scaladoc"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("display.name.missing.parameter.description"))

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = {
    new ScalaElementVisitor {
      override def visitTag(tag: ScDocTag): Unit = {
        if (!tag.isVisible(holder.getProject, holder.getFile)) return

        if (!TagNamesWithValueElement.contains(tag.name))
          return

        val valueElement = tag.getValueElement
        if (valueElement == null)
          return

        val hasDescription = !valueElement.nextSiblings.forall(el => NonDataTokens.contains(el.elementType))

        if (!hasDescription) {
          holder.registerProblem(holder.getManager.createProblemDescriptor(
            valueElement, getDisplayName.get, true,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly
          ))
        }
      }
    }
  }
}

object ScalaDocMissingParameterDescriptionInspection {

  import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing._

  private val TagNamesWithValueElement = Set(
    TagNames.Param,
    TagNames.Throws,
    TagNames.TypeParam,
  )
  private val NonDataTokens = TokenSet.create(ScalaDocTokenType.DOC_WHITESPACE, ScalaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS)
}
