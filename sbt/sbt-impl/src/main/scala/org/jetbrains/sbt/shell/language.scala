package org.jetbrains.sbt
package shell

import consulo.annotation.component.ExtensionImpl
import org.jetbrains.sbt.shell.SbtShellLanguage


import org.jetbrains.sbt.api.Factory
import org.jetbrains.sbt.api.file
import consulo.language.impl.psi.PsiFileBase
import consulo.language.ast.ASTNode
import consulo.language.{DependentLanguage, Language}
import consulo.language.parser.{ParserDefinition, PsiParser}
import consulo.language.lexer.FlexAdapter
import consulo.project.Project
import consulo.language.ast.{IElementType, IFileElementType, TokenSet}
import consulo.language.file.FileViewProvider
import consulo.language.psi.PsiElement
import consulo.language.ast.TokenType
import org.jetbrains.annotations._
import org.jetbrains.plugins.scala.api.LanguageFileTypeBase

import javax.swing.Icon

object SbtShellLanguage extends Language("sbtShell") with DependentLanguage {
  override def isCaseSensitive: Boolean = true
}

final class SbtShellTokenType(@NotNull @NonNls debugName: String) extends IElementType(debugName, SbtShellLanguage)

final class SbtShellElementType(@NotNull @NonNls debugName: String) extends IElementType(debugName, SbtShellLanguage)

@ExtensionImpl
final class SbtShellParserDefinition extends ParserDefinition {
  override def getLanguage: Language = SbtShellLanguage


  import SbtShellParserDefinition._

  override def createLexer(project: Project): FlexAdapter =
    new SbtShellLexerAdapter

  override def createParser(project: Project): PsiParser =
    new grammar.SbtShellParser

  override def createFile(viewProvider: FileViewProvider): PsiFileBase =
    new SbtShellFile(viewProvider, SbtShellFileType)

  override def createElement(node: ASTNode): PsiElement =
    grammar.SbtShellTypes.Factory.createElement(node)

  override def getFileNodeType: IFileElementType = File

  override def getWhitespaceTokens: TokenSet = WhiteSpaces

  override def getCommentTokens: TokenSet = TokenSet.EMPTY

  override def getStringLiteralElements: TokenSet = TokenSet.EMPTY
}

object SbtShellParserDefinition {

  private val WhiteSpaces = TokenSet.create(TokenType.WHITE_SPACE)
  private val File = new IFileElementType("sbt shell file", SbtShellLanguage)

  /**
   * Dummy file type required by the sbt shell LightVirtualFile
   */
  object SbtShellFileType extends LanguageFileTypeBase(SbtShellLanguage) {

    override def getDefaultExtension = "sbts"

    override def getIcon: Icon = language.SbtFileType.getIcon
  }

  private final class SbtShellLexerAdapter extends FlexAdapter(new grammar._SbtShellLexer)

  private final class SbtShellFile(viewProvider: FileViewProvider,
                                   override val getFileType: LanguageFileTypeBase)
    extends PsiFileBase(viewProvider, SbtShellLanguage)
}