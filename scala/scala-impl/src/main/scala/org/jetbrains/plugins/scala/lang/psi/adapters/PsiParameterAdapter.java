package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiParameter;

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiParameterAdapter extends PsiParameter, PsiModifierListOwnerAdapter {
    @Override
    default PsiAnnotation[] getAnnotations() {
        return psiAnnotations();
    }
}
