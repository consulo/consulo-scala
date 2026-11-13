package org.jetbrains.plugins.scalaDirective;

import consulo.language.DependentLanguage;
import consulo.language.Language;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

public class ScalaDirectiveLanguage extends Language implements DependentLanguage {

    public static final ScalaDirectiveLanguage INSTANCE = new ScalaDirectiveLanguage();

    private ScalaDirectiveLanguage() {
        super(ScalaLanguage.INSTANCE, "ScalaDirective");
    }
}
