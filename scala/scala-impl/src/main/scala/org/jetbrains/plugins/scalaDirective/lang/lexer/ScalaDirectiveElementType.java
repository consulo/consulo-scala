package org.jetbrains.plugins.scalaDirective.lang.lexer;

import consulo.language.ast.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage;

public class ScalaDirectiveElementType extends IElementType {

    public ScalaDirectiveElementType(@NonNls String debugName) {
        super(debugName, ScalaDirectiveLanguage.INSTANCE);
    }
}
