package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiAnnotationOwner;

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiAnnotatedAdapter extends PsiAnnotationOwner {
    PsiAnnotation[] psiAnnotations();

    @Override
    default PsiAnnotation[] getAnnotations() {
        return psiAnnotations();
    }
}
