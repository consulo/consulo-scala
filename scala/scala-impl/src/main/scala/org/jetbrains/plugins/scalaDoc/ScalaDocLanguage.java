package org.jetbrains.plugins.scalaDoc;

import consulo.language.DependentLanguage;
import consulo.language.Language;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

public class ScalaDocLanguage extends Language implements DependentLanguage {

    public static final ScalaDocLanguage INSTANCE = new ScalaDocLanguage();

    private ScalaDocLanguage() {
        super(ScalaLanguage.INSTANCE, "ScalaDoc");
    }
}
