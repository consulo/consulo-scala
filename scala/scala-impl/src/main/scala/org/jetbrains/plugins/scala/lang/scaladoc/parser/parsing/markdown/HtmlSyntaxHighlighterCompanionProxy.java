package org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.markdown;

import consulo.language.Language;
import com.intellij.markdown.utils.lang.HtmlSyntaxHighlighter;
import consulo.project.Project;
import consulo.application.util.HtmlChunk;

public class HtmlSyntaxHighlighterCompanionProxy {
    public static HtmlChunk colorHtmlChunk(Project project, Language language, String rawContent) {
        return HtmlSyntaxHighlighter.Companion.colorHtmlChunk(project, language, rawContent);
    }
}
