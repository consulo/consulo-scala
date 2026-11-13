package org.jetbrains.plugins.scala.finder;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.file.LanguageFileType;

@ExtensionAPI(ComponentScope.APPLICATION)
abstract public class ScalaLanguageDerivative {

    private static final ExtensionPointName<ScalaLanguageDerivative> EP_NAME =
            ExtensionPointName.create(ScalaLanguageDerivative.class);

    protected abstract LanguageFileType getFileType();

    public static boolean existsFor(LanguageFileType fileType) {
        return EP_NAME
                .getExtensionList()
                .stream()
                .map(ScalaLanguageDerivative::getFileType)
                .anyMatch(fileType::equals);
    }
}
