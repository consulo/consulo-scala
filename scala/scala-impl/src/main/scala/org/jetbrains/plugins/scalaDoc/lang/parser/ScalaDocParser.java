package org.jetbrains.plugins.scalaDoc.lang.parser;

import consulo.language.version.LanguageVersion;
import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.ast.IElementType;
import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing;
import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.ScaladocMarkdownParsing;

public class ScalaDocParser implements PsiParser {
  private final int tabSize;
  private final boolean isMarkdown;

  public ScalaDocParser(int tabSize, boolean isMarkdown) {
    this.tabSize = tabSize;
    this.isMarkdown = isMarkdown;
  }

  public void parseLight(IElementType root, PsiBuilder builder) {
    if (isMarkdown) {
      ScaladocMarkdownParsing.parse(builder, root);
    } else {
      new MyScaladocParsing(builder, tabSize).parse(root);
    }
  }

  @Override
  public ASTNode parse(IElementType root, PsiBuilder builder, LanguageVersion languageVersion) {
    parseLight(root, builder);
    return builder.getTreeBuilt();
  }
}
