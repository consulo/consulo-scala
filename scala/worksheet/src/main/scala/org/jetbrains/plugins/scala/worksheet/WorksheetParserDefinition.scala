package org.jetbrains.plugins.scala
package worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.file.FileViewProvider
import consulo.language.version.LanguageVersion
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.parser.{ScalaParser, ScalaParserDefinitionBase}
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubFileElementType

@ExtensionImpl
final class WorksheetParserDefinition extends ScalaParserDefinitionBase {

  override def getLanguage: Language = WorksheetLanguage.INSTANCE

  override def createLexer(languageVersion: LanguageVersion) =
    new ScalaLexer(/*isScala3 =*/ false, null)

  override def createParser(languageVersion: LanguageVersion): ScalaParser =
    new ScalaParser(isScala3 = false)

  override def getFileNodeType: ScStubFileElementType =
    WorksheetParserDefinition.FileNodeType

  override def createFile(viewProvider: FileViewProvider) =
    new WorksheetFile(viewProvider, WorksheetLanguage.INSTANCE)
}

object WorksheetParserDefinition {

  //noinspection TypeAnnotation
  val FileNodeType = ScStubFileElementType(WorksheetLanguage.INSTANCE)
}
