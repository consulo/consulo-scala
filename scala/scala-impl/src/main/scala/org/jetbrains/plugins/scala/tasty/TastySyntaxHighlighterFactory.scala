package org.jetbrains.plugins.scala.tasty

import consulo.annotation.component.ExtensionImpl

import consulo.virtualFileSystem.fileType.FileType
import consulo.language.editor.highlight.{SyntaxHighlighter, SyntaxHighlighterProvider}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.api.Scala3Language
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighterFactory.createScalaSyntaxHighlighter

@ExtensionImpl(id = "tasty.file")
// upstream: a file-type SyntaxHighlighterFactory; in Consulo file-type highlighters come from SyntaxHighlighterProvider
class TastySyntaxHighlighterFactory extends SyntaxHighlighterProvider {
  override def create(fileType: FileType, project: Project, file: VirtualFile): SyntaxHighlighter = {
    if (fileType == TastyFileType) createScalaSyntaxHighlighter(project, file, Scala3Language.INSTANCE)
    else null
  }
}