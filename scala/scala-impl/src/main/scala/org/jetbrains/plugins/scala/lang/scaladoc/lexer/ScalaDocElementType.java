package org.jetbrains.plugins.scala.lang.scaladoc.lexer;

import consulo.language.ast.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.plugins.scalaDoc.ScalaDocLanguage;

public class ScalaDocElementType extends IElementType {

    public ScalaDocElementType(@NonNls String debugName) {
        super(debugName, ScalaDocLanguage.INSTANCE);
  }
}
