package org.jetbrains.plugins.scala.api;

import consulo.language.DependentLanguage;
import consulo.language.InjectableLanguage;
import consulo.language.Language;

// TODO upstream implements com.intellij.lang.jvm.JvmLanguage (marker interface), which does not exist in Consulo
public class Scala3Language extends Language implements DependentLanguage, InjectableLanguage {

    public static final Scala3Language INSTANCE = new Scala3Language();

    private Scala3Language() {
        super(ScalaLanguage.INSTANCE, "Scala 3");
    }
}
