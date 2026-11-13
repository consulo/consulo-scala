package org.jetbrains.plugins.scala.findUsages;

import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.application.util.query.Query;

// upstream: @ApiStatus.Internal
public interface ExternalReferenceSearcher {
    ExtensionPointName<ExternalReferenceSearcher> EP_NAME =
            ExtensionPointName.create("org.intellij.scala.findUsages.externalReferenceSearcher");

    Query<PsiReference> search(PsiNamedElement target);

    static Query<PsiReference> searchExternally(PsiNamedElement target) {
        final var first = EP_NAME.getExtensions()[0];
        return first.search(target);
    }
}
