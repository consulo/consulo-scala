package org.jetbrains.plugins.scala.lang.parser

import consulo.language.version.LanguageVersion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language

import consulo.project.Project
import consulo.language.file.FileViewProvider
import org.jetbrains.plugins.scala.api.Scala3Language
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubFileElementType

/**
 * @see [[org.jetbrains.plugins.scala.lang.parser.ScalaParserDefinition]]
 */
@ExtensionImpl
final class Scala3ParserDefinition extends ScalaParserDefinitionBase {
  override def getLanguage: Language = Scala3Language.INSTANCE


  //noinspection TypeAnnotation
  override def getFileNodeType: ScStubFileElementType = Scala3ParserDefinition.FileNodeType

  override def createLexer(languageVersion: LanguageVersion): ScalaLexer = new ScalaLexer(true, null)

  override def createParser(languageVersion: LanguageVersion): ScalaParser = new ScalaParser(isScala3 = true)

  override def createFile(viewProvider: FileViewProvider): org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl = new ScalaFileImpl(viewProvider)
}

object Scala3ParserDefinition {

  //noinspection TypeAnnotation
  val FileNodeType: ScStubFileElementType = ScStubFileElementType(Scala3Language.INSTANCE)
}
