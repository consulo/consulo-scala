package org.jetbrains.plugins.scalaDirective
package lang
package parser

import consulo.annotation.component.ExtensionImpl
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage

import consulo.language.impl.psi.ASTWrapperPsiElement
import consulo.language.ast.ASTNode
import consulo.language.parser.ParserDefinition
import consulo.language.lexer.FlexAdapter
import consulo.language.Language
import consulo.language.version.LanguageVersion
import consulo.language.ast.{IFileElementType, TokenSet}
import consulo.language.file.FileViewProvider
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scalaDirective.lang.lexer._ScalaDirectiveLexer

@ExtensionImpl
class ScalaDirectiveParserDefinition extends ParserDefinition {

  override def getLanguage: Language = ScalaDirectiveLanguage.INSTANCE

  override val getFileNodeType = new IFileElementType(ScalaDirectiveLanguage.INSTANCE)

  override def getCommentTokens(languageVersion: LanguageVersion): TokenSet = TokenSet.EMPTY

  override def getStringLiteralElements(languageVersion: LanguageVersion): TokenSet = TokenSet.EMPTY

  override def createLexer(languageVersion: LanguageVersion): consulo.language.lexer.FlexAdapter =
    new FlexAdapter(new _ScalaDirectiveLexer(null.asInstanceOf[java.io.Reader]))

  override def createParser(languageVersion: LanguageVersion): org.jetbrains.plugins.scalaDirective.lang.parser.ScalaDirectiveParser = new ScalaDirectiveParser

  override def createElement(node: ASTNode): PsiElement = new ASTWrapperPsiElement(node)

  override def createFile(viewProvider: FileViewProvider): PsiFile = null
}
