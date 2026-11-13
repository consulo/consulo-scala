package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiTypeParameter;

public interface PsiTypeParameterAdapter extends PsiTypeParameter, PsiAnnotatedAdapter {
  @Override
  default PsiAnnotation[] getAnnotations() {
    return psiAnnotations();
  }
}
