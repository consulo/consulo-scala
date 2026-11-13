package org.jetbrains.plugins.scala.findUsages;

import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiNamedElement;

// upstream: @ApiStatus.Internal
public interface ExternalSearchScopeChecker {
    ExtensionPointName<ExternalSearchScopeChecker> EP_NAME =
            ExtensionPointName.create("org.intellij.scala.findUsages.externalSearchScopeChecker");

    boolean checkSearchScopeIsSufficient(PsiNamedElement target, UsageType usageType);

    static boolean checkSearchScopeIsSufficientExternally(PsiNamedElement target, UsageType usageType) {
        final var first = EP_NAME.getExtensions()[0];
        return first.checkSearchScopeIsSufficient(target, usageType);
    }
}
