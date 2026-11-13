package org.jetbrains.plugins.scala.lang.parser

import consulo.language.version.LanguageVersion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language

import consulo.project.Project
import consulo.language.file.FileViewProvider
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubFileElementType

/**
 * @see [[org.jetbrains.plugins.scala.lang.parser.Scala3ParserDefinition]]
 */
@ExtensionImpl
class ScalaParserDefinition extends ScalaParserDefinitionBase {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def createLexer(languageVersion: LanguageVersion): org.jetbrains.plugins.scala.lang.lexer.ScalaLexer = new ScalaLexer(false, null)

  override def createParser(languageVersion: LanguageVersion): org.jetbrains.plugins.scala.lang.parser.ScalaParser = new ScalaParser(isScala3 = false)

  //noinspection TypeAnnotation
  override def getFileNodeType: org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubFileElementType = ScalaParserDefinition.FileNodeType

  override def createFile(viewProvider: FileViewProvider): org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl = new ScalaFileImpl(viewProvider)
}

object ScalaParserDefinition {

  //noinspection TypeAnnotation
  val FileNodeType: ScStubFileElementType = ScStubFileElementType(ScalaLanguage.INSTANCE)
}
