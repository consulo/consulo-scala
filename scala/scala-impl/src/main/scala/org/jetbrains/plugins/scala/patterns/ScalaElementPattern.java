package org.jetbrains.plugins.scala.patterns;

import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.InitialPatternCondition;
import consulo.language.pattern.PatternCondition;
import com.intellij.java.language.patterns.PsiJavaElementPattern;
import com.intellij.java.language.psi.PsiMethod;
import consulo.language.util.ProcessingContext;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral;

public class ScalaElementPattern<T extends ScalaPsiElement, Self extends ScalaElementPattern<T, Self>> extends PsiJavaElementPattern<T, Self> {
  public ScalaElementPattern(Class<T> tClass) {
    super(tClass);
  }

  public ScalaElementPattern(InitialPatternCondition<T> tInitialPatternCondition) {
    super(tInitialPatternCondition);
  }

  public Self isRegExpLiteral() {
    return with(new PatternCondition<T>("isRegExpLiteral") {
      @Override
      public boolean accepts(final T literal, final ProcessingContext context) {
        return ScalaElementPatternImpl.isRegExpLiteral(literal);
      }
    });
  }

  public Self callArgument(final int index, final ElementPattern<? extends PsiMethod> methodPattern) {
    return with(new PatternCondition<T>("callArgument") {
      @Override
      public boolean accepts(final T host, final ProcessingContext context) {
        return ScalaElementPatternImpl.isMethodCallArgument(host, context, index, methodPattern);
      }
    });
  }

    public Self constructorCallArgument(final String className, final int index, final String... parameterTypes) {
        return with(new ConstructorCallArgumentPattern(className, index, parameterTypes));
    }

  public static class Capture<T extends ScLiteral> extends ScalaElementPattern<T, Capture<T>> {
    public Capture(final Class<T> aClass) {
      super(aClass);
    }

    public Capture(final InitialPatternCondition<T> condition) {
      super(condition);
    }
  }
}