package org.jetbrains.plugins.scala.lang

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage

import consulo.language.codeStyle.CodeStyle
import consulo.language.ast.{ASTNode, TokenSeparatorGenerator}
import consulo.language.ast.TokenType
import consulo.language.impl.ast.Factory
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScImportStmtElementType
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveElementType

/**
 * Here we handle cases where 2 adjacent elements of different languages require a newline or other whitespace separator.
 *
 * Note that cases of same-language adjacent elements are handled by [[com.intellij.lang.LanguageTokenSeparatorGenerators]]
 */
@ExtensionImpl
class ScalaTokenSeparatorGenerator extends TokenSeparatorGenerator {
  override def getLanguage: Language = ScalaDirectiveLanguage.INSTANCE


  override def generateWhitespaceBetweenTokens(left: ASTNode, right: ASTNode): ASTNode = {

    if (left.getElementType.is[ScalaDirectiveElementType] && right.getTreeParent.getElementType.is[ScImportStmtElementType]) {

      val leftPsi = left.getPsi
      val project = leftPsi.getProject
      val commonCodeStyleSettings = CodeStyle.getSettings(project).getCommonSettings(ScalaLanguage.INSTANCE)
      val repeatCount = commonCodeStyleSettings.BLANK_LINES_BEFORE_IMPORTS + 1
      val psiManager = leftPsi.getManager

      Factory.createSingleLeafElement(TokenType.WHITE_SPACE, "\n".repeat(repeatCount), 0, repeatCount, null, psiManager)
    } else {
      null
    }
  }
}
