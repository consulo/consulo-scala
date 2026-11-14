package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.application.progress.ProgressManager
import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.util.ExternalSystemCrcCalculator
import consulo.language.ast.IElementType
import consulo.language.file.LanguageFileType
import consulo.language.lexer.{LayeredLexer, Lexer}
import consulo.language.parser.ParserDefinition
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.bsp.BSP
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveElementType
import org.jetbrains.plugins.scalaDirective.lang.parser.ScalaDirectiveElementTypes

import java.util.zip.CRC32

/**
 * The CRC calculator used for computing CRC values for scala affected external project files in the BSP external system.
 * In practice, it is only applicable to Scala CLI because only there the affected external project files are Scala files.
 */
// upstream: extends AbstractCrcCalculator (internal in Consulo) - the token CRC loop is inlined
@ExtensionImpl
class BspScalaFilesCrcCalculator extends ExternalSystemCrcCalculator {

  override def isApplicable(projectSystemId: ProjectSystemId, virtualFile: VirtualFile): Boolean =
    projectSystemId == BSP.ProjectSystemId && virtualFile.getFileType == ScalaFileType.INSTANCE

  private def isIgnoredToken(iElementType: IElementType, charSequence: CharSequence, parserDefinition: ParserDefinition): Boolean =
    iElementType match {
      case _: ScalaDirectiveElementType => false
      case _ => true
    }

  private def createLexer(project: Project, parserDefinition: ParserDefinition): Lexer = {
    val scalaLexer = parserDefinition.createLexer(parserDefinition.getLanguage.getVersions()(0))
    val scalaDirectiveParser = ParserDefinition.forLanguage(ScalaDirectiveLanguage.INSTANCE)
    val scalaDirectiveLexer = scalaDirectiveParser.createLexer(ScalaDirectiveLanguage.INSTANCE.getVersions()(0))
    new LayeredLexer(scalaLexer) {
      locally {
        registerSelfStoppingLayer(scalaDirectiveLexer, Array(ScalaDirectiveElementTypes.SCALA_DIRECTIVE), IElementType.EMPTY_ARRAY)
      }
    }
  }

  override def calculateCrc(project: Project, file: VirtualFile, fileText: CharSequence): java.lang.Long = {
    val parserDefinition = file.getFileType match {
      case languageFileType: LanguageFileType => ParserDefinition.forLanguage(languageFileType.getLanguage)
      case _ => null
    }
    if (parserDefinition == null) return null

    val lexer = createLexer(project, parserDefinition)
    val crc32 = new CRC32()
    lexer.start(fileText)
    ProgressManager.checkCanceled()
    while (lexer.getTokenType != null) {
      val tokenType = lexer.getTokenType
      val tokenText = fileText.subSequence(lexer.getTokenStart, lexer.getTokenEnd)
      if (!isIgnoredToken(tokenType, tokenText, parserDefinition) && !tokenText.toString.isBlank) {
        crc32.update(tokenText.length)
        tokenText.chars().forEach(c => crc32.update(c))
      }
      lexer.advance()
      ProgressManager.checkCanceled()
    }
    crc32.getValue
  }
}
