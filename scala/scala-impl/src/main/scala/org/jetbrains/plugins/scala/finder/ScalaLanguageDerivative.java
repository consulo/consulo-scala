package org.jetbrains.plugins.scala.finder;

import consulo.component.extension.ExtensionPointName;
import consulo.language.file.LanguageFileType;

abstract public class ScalaLanguageDerivative {

    private static final ExtensionPointName<ScalaLanguageDerivative> EP_NAME =
            ExtensionPointName.create("org.intellij.scala.scalaLanguageDerivative");

    protected abstract LanguageFileType getFileType();

    public static boolean existsFor(LanguageFileType fileType) {
        return EP_NAME
                .getExtensionList()
                .stream()
                .map(ScalaLanguageDerivative::getFileType)
                .anyMatch(fileType::equals);
    }
}
