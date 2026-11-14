package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.context.{FileTypeBasedContextType, TemplateActionContext}
import consulo.language.editor.highlight.SyntaxHighlighter
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighterFactory
import org.jetbrains.plugins.scala.api.{Scala3Language, ScalaFileType}

@ExtensionImpl
final class Scala3FileTemplateContextType extends FileTypeBasedContextType(
  "SCALA3",
  Scala3Language.INSTANCE.getDisplayName,
  ScalaFileType.INSTANCE
)  {

  override def createHighlighter(): SyntaxHighlighter =
    ScalaSyntaxHighlighterFactory.createScalaSyntaxHighlighter(null, null, Scala3Language.INSTANCE)

  override def isInContext(templateActionContext: TemplateActionContext): Boolean =
    templateActionContext.getFile.getLanguage == Scala3Language.INSTANCE
}