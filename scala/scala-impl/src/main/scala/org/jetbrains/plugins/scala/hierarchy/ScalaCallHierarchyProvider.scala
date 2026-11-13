package org.jetbrains.plugins.scala.hierarchy

import com.intellij.java.impl.ide.hierarchy.call.{JavaCallHierarchyModel, JavaCallHierarchyProvider}
import com.intellij.java.language.psi.PsiMethod
import consulo.language.Language
import consulo.language.editor.hierarchy.{HierarchyModel, HierarchyRequest, HierarchyTreeStructure, StandardHierarchyViewTypes}
import consulo.project.Project
import org.jetbrains.plugins.scala.api.ScalaLanguage

// upstream: the call hierarchy view was customized by ScalaCallHierarchyBrowser (CallHierarchyBrowserBase),
// in Consulo the view is driven by the HierarchyModel
class ScalaCallHierarchyProvider extends JavaCallHierarchyProvider {
  override def createModel(project: Project, target: PsiMethod): HierarchyModel[PsiMethod] =
    new ScalaCallHierarchyModel(project, target)

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

private class ScalaCallHierarchyModel(project: Project, target: PsiMethod) extends JavaCallHierarchyModel(project, target) {
  override def createTreeStructure(request: HierarchyRequest[PsiMethod]): HierarchyTreeStructure = {
    val viewType = request.getViewType
    if (StandardHierarchyViewTypes.CALLER == viewType)
      new ScalaCallerMethodsTreeStructure(project, request.getTarget, request.getScope)
    else if (StandardHierarchyViewTypes.CALLEE == viewType)
      new ScalaCalleeMethodsTreeStructure(project, request.getTarget, request.getScope)
    else null
  }
}
