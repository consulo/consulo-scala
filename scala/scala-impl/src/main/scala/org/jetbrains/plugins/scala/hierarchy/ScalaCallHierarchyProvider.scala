package org.jetbrains.plugins.scala.hierarchy

import com.intellij.java.impl.ide.hierarchy.call.JavaCallHierarchyProvider
import com.intellij.ide.hierarchy.{CallHierarchyBrowserBase, HierarchyBrowser}
import consulo.language.psi.PsiElement
import com.intellij.java.language.psi.PsiMethod

class ScalaCallHierarchyProvider extends JavaCallHierarchyProvider {
  override def browserActivated(hierarchyBrowser: HierarchyBrowser): Unit = {
    hierarchyBrowser.asInstanceOf[ScalaCallHierarchyBrowser].changeView(CallHierarchyBrowserBase.getCallerType)
  }

  override def createHierarchyBrowser(target: PsiElement): HierarchyBrowser = {
    new ScalaCallHierarchyBrowser(target.getProject, target.asInstanceOf[PsiMethod])
  }
}