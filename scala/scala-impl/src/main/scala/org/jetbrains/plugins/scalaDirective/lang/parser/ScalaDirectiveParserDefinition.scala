package org.jetbrains.plugins.scalaDirective
package lang
package parser

import consulo.language.impl.psi.ASTWrapperPsiElement
import consulo.language.ast.ASTNode
import consulo.language.parser.ParserDefinition
import consulo.language.lexer.FlexAdapter
import consulo.project.Project
import consulo.language.ast.{IFileElementType, TokenSet}
import consulo.language.file.FileViewProvider
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scalaDirective.lang.lexer._ScalaDirectiveLexer

class ScalaDirectiveParserDefinition extends ParserDefinition {

  override val getFileNodeType = new IFileElementType(ScalaDirectiveLanguage.INSTANCE)

  //noinspection TypeAnnotation
  override val getCommentTokens = TokenSet.create()

  //noinspection TypeAnnotation
  override val getStringLiteralElements = TokenSet.create()

  override def createLexer(project: Project): consulo.language.lexer.FlexAdapter =
    new FlexAdapter(new _ScalaDirectiveLexer(null.asInstanceOf[java.io.Reader]))

  override def createParser(project: Project): org.jetbrains.plugins.scalaDirective.lang.parser.ScalaDirectiveParser = new ScalaDirectiveParser

  override def createElement(node: ASTNode): PsiElement = new ASTWrapperPsiElement(node)

  override def createFile(viewProvider: FileViewProvider): PsiFile = null
}
