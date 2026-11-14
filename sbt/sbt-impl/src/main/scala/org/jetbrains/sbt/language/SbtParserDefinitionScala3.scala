package org.jetbrains.sbt.language

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language


import org.jetbrains.sbt.api.language.SbtLanguageScala3
import consulo.language.parser.PsiParser
import consulo.language.lexer.Lexer
import consulo.project.Project
import consulo.language.file.FileViewProvider
import consulo.language.ast.IFileElementType
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer
import org.jetbrains.plugins.scala.lang.parser.{ScalaParser, ScalaParserDefinitionBase}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
final class SbtParserDefinitionScala3 extends ScalaParserDefinitionBase {
  override def getLanguage: Language = SbtLanguageScala3.INSTANCE

  override def createFile(viewProvider: FileViewProvider): ScalaFile = new SbtFileImpl(viewProvider)

  override def createLexer(project: Project): Lexer = new ScalaLexer(/* isScala3 = */ true, project)

  override def createParser(project: Project): PsiParser = new ScalaParser(isScala3 = true)

  override val getFileNodeType: IFileElementType = new IFileElementType("sbt.FILE", SbtLanguageScala3.INSTANCE)
}
