package org.jetbrains.plugins.scalaDoc
package lang
package parser

import consulo.language.version.LanguageVersion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scalaDoc.ScalaDocLanguage

import consulo.language.codeStyle.CodeStyle
import consulo.language.impl.psi.ASTWrapperPsiElement
import consulo.language.ast.ASTNode
import consulo.language.parser.{ParserDefinition, PsiParser}
import consulo.language.lexer.Lexer
import consulo.project.Project
import consulo.language.ast.{IFileElementType, TokenSet}
import consulo.language.file.FileViewProvider
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.docsyntax.ScalaDocSyntaxElementType
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.{ScalaDocLexer, ScalaDocMarkdownLexer, ScalaDocTokenType}
import org.jetbrains.plugins.scala.lang.scaladoc.parser.ScalaDocElementTypes
import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing.TagNames
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.{ScDocComment, ScDocInlinedTag, ScDocTag}
import org.jetbrains.plugins.scala.lang.scaladoc.psi.impl._
import org.jetbrains.plugins.scalaDoc.lang.parser.ScalaDocParserDefinition.isInsideJavaLinkTag

@ExtensionImpl
class ScalaDocParserDefinition extends ParserDefinition {
  override def getLanguage: Language = ScalaDocLanguage.INSTANCE


  import ScalaDocElementTypes._
  import ScalaDocParserDefinition.isInsideThrowsTag
  import ScalaDocTokenType._

  override val getFileNodeType = new IFileElementType(ScalaDocLanguage.INSTANCE)

  //noinspection TypeAnnotation
  override def getCommentTokens(languageVersion: LanguageVersion) = TokenSet.create(SCALA_DOC_COMMENT)

  //noinspection TypeAnnotation
  override def getStringLiteralElements(languageVersion: LanguageVersion) = TokenSet.create()

  def createLexerWithFlavour(project: Project, isMarkdown: Boolean): Lexer = if (isMarkdown) new ScalaDocMarkdownLexer else new ScalaDocLexer

  override def createLexer(languageVersion: LanguageVersion): Lexer = createLexerWithFlavour(null, isMarkdown = false /* fallback is no Markdown */)

  def createParserWithFlavour(project: Project, isMarkdown: Boolean): PsiParser = {
    val tabSize = CodeStyle.getProjectOrDefaultSettings(project).getLanguageIndentOptions(ScalaLanguage.INSTANCE).TAB_SIZE
    new ScalaDocParser(tabSize, isMarkdown)
  }

  override def createParser(languageVersion: LanguageVersion): PsiParser = createParserWithFlavour(null, isMarkdown = false /* fallback is no Markdown */)

  /**
   * see also [[org.jetbrains.plugins.scala.lang.parser.ScalaASTFactory.createLeaf]]
   * @inheritdoc
   */
  override def createElement(node: ASTNode): PsiElement = node.getElementType match {
    case DOC_INNER_CODE_TAG   => new ScDocInnerCodeElementImpl(node)
    case DOC_INLINED_TAG      => new ScDocInlinedTagImpl(node)
    case DOC_PARAM_REF        => new ScDocParamRefImpl(node)
    case DOC_METHOD_REF       => new ScDocMethodRefImpl(node)
    case DOC_FIELD_REF        => new ScDocFieldRefImpl(node)
    case DOC_METHOD_PARAMS    => new ScDocMethodParamsImpl(node)
    case DOC_METHOD_PARAMETER => new ScDocMethodParameterImpl(node)
    case DOC_CODE_LINK_VALUE  => new ScDocResolvableCodeReferenceImpl(node)
    case DOC_TAG              => new ScDocTagImpl(node)
    case DOC_TAG_VALUE_TOKEN                   =>
      if (isInsideThrowsTag(node)) new ScDocThrowTagValueImpl(node)
      else if (isInsideJavaLinkTag(node)) new ScDocResolvableCodeReferenceImpl(node)
      else new ScDocTagValueImpl(node)
    case syntaxType: ScalaDocSyntaxElementType =>
      val result = new ScDocSyntaxElementImpl(node)
      result.setFlag(syntaxType.getFlagConst)

      var parentNode = node
      while (parentNode.getTreeParent != null &&
        parentNode.getElementType != SCALA_DOC_COMMENT) {

        parentNode = parentNode.getTreeParent
        parentNode.getElementType match {
          case elementType: ScalaDocSyntaxElementType =>
            result.setFlag(elementType.getFlagConst)
          case _ =>
        }
      }

      result
    case DOC_LIST      => new ScDocListImpl(node)
    case DOC_LIST_ITEM => new ScDocListItemImpl(node)
    case DOC_PARAGRAPH => new ScDocParagraphImpl(node)
    case DOC_CODEBLOCK => new ScDocInnerCodeElementImpl(node)
    case DOC_MARKDOWN_HEADER => new ScDocMarkdownHeaderImpl(node)
    case ScalaDocElementTypes.DOC_BLOCKQUOTE => new ScDocBlockQuoteImpl(node)
    case _             => new ASTWrapperPsiElement(node)
  }

  override def createFile(viewProvider: FileViewProvider): PsiFile = null
}

object ScalaDocParserDefinition {

  private def isInsideThrowsTag(node: ASTNode): Boolean = {
    var parent = node.getTreeParent

    while (parent != null && parent.getPsi != null && !parent.getPsi.is[ScDocTag, ScDocComment])
      parent = parent.getTreeParent

    parent != null && (parent.getPsi match {
      case tag: ScDocTag => tag.name == TagNames.Throws
      case _             => false
    })
  }

  private def isInsideJavaLinkTag(node: ASTNode): Boolean = {
    var parent = node.getTreeParent

    while (parent != null && parent.getPsi != null && !parent.getPsi.is[ScDocInlinedTag, ScDocComment] )
      parent = parent.getTreeParent

    parent != null && (parent.getPsi match {
      case inlineTag: ScDocInlinedTag =>
        val tagName = inlineTag.name
        tagName == TagNames.JavaLink ||
          tagName == TagNames.JavaLinkPlain
      case _             => false
    })
  }
}
