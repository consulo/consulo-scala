package org.jetbrains.plugins.scala.findUsages;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.application.util.query.Query;

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalReferenceSearcher {
    ExtensionPointName<ExternalReferenceSearcher> EP_NAME =
            ExtensionPointName.create(ExternalReferenceSearcher.class);

    Query<PsiReference> search(PsiNamedElement target);

    static Query<PsiReference> searchExternally(PsiNamedElement target) {
        final var first = EP_NAME.getExtensions()[0];
        return first.search(target);
    }
}
