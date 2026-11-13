package org.jetbrains.plugins.scalaDirective.lang.lexer;

import consulo.language.ast.IElementType;

public interface ScalaDirectiveTokenTypes {
    IElementType tDIRECTIVE_PREFIX = new ScalaDirectiveElementType("tDIRECTIVE_PREFIX");
    IElementType tDIRECTIVE_COMMA = new ScalaDirectiveElementType("tDIRECTIVE_COMMA");
    IElementType tDIRECTIVE_COMMAND = new ScalaDirectiveElementType("tDIRECTIVE_COMMAND");
    IElementType tDIRECTIVE_KEY = new ScalaDirectiveElementType("tDIRECTIVE_KEY");
    IElementType tDIRECTIVE_VALUE = new ScalaDirectiveElementType("tDIRECTIVE_VALUE");
    IElementType tDIRECTIVE_ERROR = new ScalaDirectiveElementType("tDIRECTIVE_ERROR");
}
