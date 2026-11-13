package org.jetbrains.plugins.scala.lang.scaladoc.reflinks

import consulo.language.impl.psi.ASTWrapperPsiElement
import consulo.language.ast.ASTNode
import consulo.language.parser.{ParserDefinition, PsiParser}
import consulo.language.lexer.Lexer
import consulo.project.Project
import consulo.language.ast.{IFileElementType, TokenSet}
import consulo.language.file.FileViewProvider
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.lang.scaladoc.reflinks.lexer.ScalaDocRefLinkLexer
import org.jetbrains.plugins.scala.lang.scaladoc.reflinks.parser.ScalaDocRefLinkParser
import org.jetbrains.plugins.scala.lang.scaladoc.reflinks.psi.ScalaDocRefLinkFile
import org.jetbrains.plugins.scala.lang.scaladoc.reflinks.psi.impl.{ScDocRefQuerySegmentImpl, ScDocRefStrictMemberIdQueryImpl, ScDocRefThisQueryImpl, ScPackageQueryImpl}

class ScalaDocRefLinkParserDefinition extends ParserDefinition {
  override def createLexer(project: Project): Lexer = new ScalaDocRefLinkLexer

  override def createParser(project: Project): PsiParser = new ScalaDocRefLinkParser

  override def getFileNodeType: IFileElementType = ScalaDocRefLinkParserDefinition.FileNodeType

  @NotNull
  override def getCommentTokens: TokenSet = TokenSet.EMPTY

  @NotNull
  override def getStringLiteralElements: TokenSet = TokenSet.EMPTY

  @NotNull
  override def createElement(node: ASTNode): PsiElement =
    node.getElementType match {
      case ScalaDocRefLinkElementTypes.STRICT_MEMBER_ID => new ScDocRefStrictMemberIdQueryImpl(node)
      case ScalaDocRefLinkElementTypes.QUERY_SEGMENT => new ScDocRefQuerySegmentImpl(node)
      case ScalaDocRefLinkElementTypes.THIS_QUERY_SEGMENT => new ScDocRefThisQueryImpl(node)
      case ScalaDocRefLinkElementTypes.THIS_PACKAGE_SEGMENT => new ScPackageQueryImpl(node)
      case _ => new ASTWrapperPsiElement(node)
    }

  override def createFile(viewProvider: FileViewProvider): PsiFile = new ScalaDocRefLinkFile(viewProvider)
}

object ScalaDocRefLinkParserDefinition {
  val FileNodeType: IFileElementType = new IFileElementType(ScalaDocRefLinkLanguage.INSTANCE)
}
