package org.jetbrains.plugins.scala.util;

import consulo.language.editor.intention.BaseIntentionAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.inspection.InspectionTool;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;

@ExtensionAPI(ComponentScope.APPLICATION)
abstract public class IntentionAvailabilityChecker {
  private static final ExtensionPointName<IntentionAvailabilityChecker> EP_NAME =
      ExtensionPointName.create(IntentionAvailabilityChecker.class);

  private static final IntentionAvailabilityChecker defaultChecker = new IntentionAvailabilityChecker() {};
  
  public boolean isIntentionAvailable(BaseIntentionAction intention, PsiElement psiElement) {
    return true;
  }
  
  public boolean isInspectionAvailable(InspectionTool inspection, PsiElement psiElement) {
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
  
  public static boolean checkInspection(InspectionTool inspection, PsiElement psiElement) {
    return getChecker(psiElement).isInspectionAvailable(inspection, psiElement);
  }
}
