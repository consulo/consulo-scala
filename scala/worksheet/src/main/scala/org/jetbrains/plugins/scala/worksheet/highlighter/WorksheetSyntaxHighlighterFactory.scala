package org.jetbrains.plugins.scala.worksheet.highlighter

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.highlight.SyntaxHighlighterFactory
import consulo.language.psi.LanguageSubstitutors
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.highlighter.{ScalaSyntaxHighlighter, ScalaSyntaxHighlighterFactory}
import org.jetbrains.plugins.scala.worksheet.{WorksheetLanguage, WorksheetLanguage3}

abstract class WorksheetSyntaxHighlighterFactoryBase extends SyntaxHighlighterFactory {

  override def getSyntaxHighlighter(project: Project, file: VirtualFile): ScalaSyntaxHighlighter = {
    val language = if (project == null || file == null)
      WorksheetLanguage.INSTANCE
    else
      LanguageSubstitutors.substituteLanguage(WorksheetLanguage.INSTANCE, file, project)

    ScalaSyntaxHighlighterFactory.createScalaSyntaxHighlighter(project, file, language)
  }
}

@ExtensionImpl
final class WorksheetSyntaxHighlighterFactory extends WorksheetSyntaxHighlighterFactoryBase {
  override def getLanguage: Language = WorksheetLanguage.INSTANCE
}

@ExtensionImpl
final class WorksheetSyntaxHighlighterFactory3 extends WorksheetSyntaxHighlighterFactoryBase {
  override def getLanguage: Language = WorksheetLanguage3.INSTANCE
}
