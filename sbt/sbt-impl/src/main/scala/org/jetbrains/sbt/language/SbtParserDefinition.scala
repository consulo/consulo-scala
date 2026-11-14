package org.jetbrains.sbt
package language

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language


import org.jetbrains.sbt.api.language.SbtLanguage
import consulo.language.version.LanguageVersion
import consulo.language.file.FileViewProvider
import consulo.language.ast.IFileElementType
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.parser.{ScalaParser, ScalaParserDefinitionBase}

@ExtensionImpl
final class SbtParserDefinition extends ScalaParserDefinitionBase {
  override def getLanguage: Language = SbtLanguage.INSTANCE


  override def createLexer(languageVersion: LanguageVersion) = new ScalaLexer(false, null)

  override def createParser(languageVersion: LanguageVersion) = new ScalaParser(isScala3 = false)

  override val getFileNodeType = new IFileElementType("sbt.FILE", SbtLanguage.INSTANCE)

  override def createFile(viewProvider: FileViewProvider) = new SbtFileImpl(viewProvider)
}
