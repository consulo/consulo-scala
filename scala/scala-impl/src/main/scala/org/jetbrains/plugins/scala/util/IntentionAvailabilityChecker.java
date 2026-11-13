package org.jetbrains.plugins.scala.util;

import consulo.language.editor.intention.BaseIntentionAction;
import com.intellij.codeInspection.InspectionProfileEntry;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;

abstract public class IntentionAvailabilityChecker {
  private static final ExtensionPointName<IntentionAvailabilityChecker> EP_NAME =
      ExtensionPointName.create("org.intellij.scala.scalaIntentionAvailabilityChecker");

  private static final IntentionAvailabilityChecker defaultChecker = new IntentionAvailabilityChecker() {};
  
  public boolean isIntentionAvailable(BaseIntentionAction intention, PsiElement psiElement) {
    return true;
  }
  
  public boolean isInspectionAvailable(InspectionProfileEntry inspection, PsiElement psiElement) {
    return true;
  }
  
  public boolean canCheck(PsiElement psiElement) {
    return false;
  }
  
  private static IntentionAvailabilityChecker getChecker(PsiElement psiElement) {
    for (IntentionAvailabilityChecker checker : EP_NAME.getExtensions()) {
      if (checker.canCheck(psiElement)) return checker;
    }
    
    return defaultChecker;
  }
  
  public static boolean checkIntention(BaseIntentionAction intention, PsiElement psiElement) {
    return getChecker(psiElement).isIntentionAvailable(intention, psiElement);
  }
  
  public static boolean checkInspection(InspectionProfileEntry inspection, PsiElement psiElement) {
    return getChecker(psiElement).isInspectionAvailable(inspection, psiElement);
  }
}
