package org.jetbrains.plugins.scala.api;

import consulo.language.DependentLanguage;
import consulo.language.InjectableLanguage;
import consulo.language.Language;
import com.intellij.java.language.jvm.JvmLanguage;

public class Scala3Language extends Language implements JvmLanguage, DependentLanguage, InjectableLanguage {

    public static final Scala3Language INSTANCE = new Scala3Language();

    private Scala3Language() {
        super(ScalaLanguage.INSTANCE, "Scala 3");
    }
}
