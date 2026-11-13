package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiJvmModifiersOwner;

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiModifierListOwnerAdapter extends PsiJvmModifiersOwner, PsiAnnotatedAdapter {
    @Override
    default PsiAnnotation[] getAnnotations() {
        return psiAnnotations();
    }

    @Override
    default boolean hasAnnotation(String fqn) {
        return PsiJvmModifiersOwner.super.hasAnnotation(fqn);
    }

    // upstream: hasAnnotations() disambiguated JvmAnnotatedElement/PsiAnnotationOwner; in Consulo only PsiAnnotationOwner declares it
}
