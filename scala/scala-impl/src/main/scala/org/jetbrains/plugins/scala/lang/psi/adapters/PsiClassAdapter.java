package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiMethod;

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiClassAdapter extends PsiClass, PsiTypeParametersOwnerAdapter {
    PsiField[] psiFields();

    PsiMethod[] psiMethods();

    PsiClass[] psiInnerClasses();

    @Override
    default PsiField[] getFields() {
        return psiFields();
    }

    @Override
    default PsiMethod[] getMethods() {
        return psiMethods();
    }

    @Override
    default PsiClass[] getInnerClasses() {
        return psiInnerClasses();
    }
}
