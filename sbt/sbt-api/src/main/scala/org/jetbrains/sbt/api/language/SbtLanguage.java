package org.jetbrains.sbt.api.language;

import consulo.language.Language;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

public final class SbtLanguage extends Language {

    public static final SbtLanguage INSTANCE = new SbtLanguage();

    private SbtLanguage() {
        super(ScalaLanguage.INSTANCE, "sbt");
    }
}
