package org.jetbrains.plugins.scala.annotator.gutter

import consulo.language.editor.gutter.GutterIconNavigationHandler
import consulo.language.editor.localize.CodeInsightLocalize
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.PsiElement
import consulo.language.psi.search.DefinitionsScopedSearch
import consulo.project.DumbService
import consulo.ui.event.ComponentEvent
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * Java version is in [[com.intellij.codeInsight.daemon.impl.InheritorsLineMarkerNavigator]]
 * that delegates to [[com.intellij.codeInsight.navigation.GotoImplementationHandler]]
 */
private class ScalaInheritorsLineMarkerNavigator extends GutterIconNavigationHandler[PsiElement] {
  override def navigate(event: ComponentEvent[?], element: PsiElement): Unit = {
    val parent = element.getParent

    // upstream: @NlsContexts.PopupContent
    val dumbModeMessage= parent match {
      case _: PsiClass =>
        ScalaBundle.message("notification.navigation.to.overriding.classes")
      case _=>
        ScalaBundle.message("notification.navigation.to.overriding.members")
    }

    // upstream: new GotoImplementationHandler().navigateToImplementations(parent, event, dumbModeMessage) (not public in Consulo)
    val project = parent.getProject
    if (DumbService.isDumb(project)) {
      DumbService.getInstance(project).showDumbModeNotification(dumbModeMessage)
      return
    }

    val implementations = DefinitionsScopedSearch.search(parent).findAll().toArray(PsiElement.EMPTY_ARRAY)
    val title = CodeInsightLocalize.gotoImplementationFindusagesTitle(element.getText).get
    ScalaNavigationUtils.navigate(event, implementations, project, title, title, ScalaMarkerType.newPresentationProvider)
  }
}
