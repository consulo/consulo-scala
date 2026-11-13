package org.jetbrains.plugins.scala.tasty

import consulo.annotation.component.ExtensionImpl

import consulo.virtualFileSystem.fileType.FileType
import consulo.language.editor.highlight.{SyntaxHighlighter, SyntaxHighlighterFactory, SyntaxHighlighterProvider}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.api.Scala3Language
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighterFactory.createScalaSyntaxHighlighter

@ExtensionImpl(id = "tasty.file")
class TastySyntaxHighlighterFactory extends SyntaxHighlighterFactory with SyntaxHighlighterProvider {
  override def getSyntaxHighlighter(project: Project, virtualFile: VirtualFile): SyntaxHighlighter = null

  override def create(fileType: FileType, project: Project, file: VirtualFile): SyntaxHighlighter = {
    if (fileType == TastyFileType) createScalaSyntaxHighlighter(project, file, Scala3Language.INSTANCE)
    else null
  }
}