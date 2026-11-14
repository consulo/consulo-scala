package org.jetbrains.plugins.scala.worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.file.FileViewProvider
import consulo.language.version.LanguageVersion
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.parser.{ScalaParser, ScalaParserDefinitionBase}
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubFileElementType

@ExtensionImpl
final class WorksheetParserDefinition3 extends ScalaParserDefinitionBase {

  override def getLanguage: Language = WorksheetLanguage3.INSTANCE

  override def createLexer(languageVersion: LanguageVersion) =
    new ScalaLexer(/*isScala3 =*/ true, null)

  override def createParser(languageVersion: LanguageVersion): ScalaParser =
    new ScalaParser(isScala3 = true)

  override def getFileNodeType: ScStubFileElementType =
    WorksheetParserDefinition3.FileNodeType

  override def createFile(viewProvider: FileViewProvider) =
    new WorksheetFile(viewProvider, WorksheetLanguage3.INSTANCE)
}

object WorksheetParserDefinition3 {

  //noinspection TypeAnnotation
  val FileNodeType = ScStubFileElementType(WorksheetLanguage3.INSTANCE)
}
