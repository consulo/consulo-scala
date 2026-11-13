package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiTypeParameter;
import com.intellij.java.language.psi.PsiTypeParameterListOwner;

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiTypeParametersOwnerAdapter extends PsiTypeParameterListOwner {
    PsiTypeParameter[] psiTypeParameters();

    @Override
    default PsiTypeParameter[] getTypeParameters() {
        return psiTypeParameters();
    }
}
