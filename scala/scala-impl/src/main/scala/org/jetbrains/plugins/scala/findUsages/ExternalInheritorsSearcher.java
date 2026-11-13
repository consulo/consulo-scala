package org.jetbrains.plugins.scala.findUsages;

import consulo.component.extension.ExtensionPointName;
import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.application.util.query.Query;

// upstream: @ApiStatus.Internal
public interface ExternalInheritorsSearcher {
    ExtensionPointName<ExternalInheritorsSearcher> EP_NAME =
            ExtensionPointName.create("org.intellij.scala.findUsages.externalInheritorsSearcher");

    Query<PsiElement> search(PsiClass cls, SearchScope scope, boolean checkDeep);

    static Query<PsiElement> searchExternally(PsiClass cls, SearchScope scope, boolean checkDeep) {
        final var first = EP_NAME.getExtensions()[0];
        return first.search(cls, scope, checkDeep);
    }
}
