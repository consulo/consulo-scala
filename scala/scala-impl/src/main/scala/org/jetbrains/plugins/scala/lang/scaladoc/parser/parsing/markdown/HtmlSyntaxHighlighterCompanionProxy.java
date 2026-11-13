package org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.markdown;

import consulo.language.Language;
import consulo.language.editor.codeBlock.CodeBlockHighlighter;
import consulo.project.Project;
import consulo.application.util.HtmlChunk;

public class HtmlSyntaxHighlighterCompanionProxy {
    // upstream: HtmlSyntaxHighlighter.Companion.colorHtmlChunk (platform markdown-utils, not in Consulo);
    //           Consulo highlights code blocks for documentation with CodeBlockHighlighter
    public static HtmlChunk colorHtmlChunk(Project project, Language language, String rawContent) {
        return HtmlChunk.raw(project.getApplication().getInstance(CodeBlockHighlighter.class).newHtmlBuilder(project, language, rawContent).build());
    }
}
