package org.jetbrains.plugins.scala.lang.psi.impl.search

import consulo.module
import consulo.language.util.ModuleUtilCore
import consulo.language.psi.scope.{GlobalSearchScope, LocalSearchScope}
import consulo.content.scope.SearchScope
import consulo.language.psi.UseScopeEnlarger
import consulo.language.psi.PsiElement
import com.intellij.java.language.psi.PsiMember
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.project.ModuleExt

// upstream: @ApiStatus.Internal
class ScalaSharedSourcesUseScopeEnlarger extends UseScopeEnlarger {
  override def getAdditionalUseScope(element: PsiElement): SearchScope = {
    val psiMember = element match {
      case m: PsiMember => m
      case _ =>
        return null
    }

    val psiFile = psiMember.getContainingFile
    if (psiFile == null)
      return null

    val sharedModules: Array[module.Module] = cachedInUserData("getAdditionalUseScope.sharedModules", psiFile, ScalaPsiManager.instance(psiFile.getProject).TopLevelModificationTracker, Tuple1(psiFile)) {
      val module = ModuleUtilCore.findModuleForPsiElement(psiFile)
      if (module == null) Array.empty else module.sharedSourceDependencies.toArray
    }

    if (sharedModules.isEmpty)
      return null

    val useScope = psiMember.getUseScope
    //If member has local search scope we know it can't be used in shared sources
    if (useScope.isInstanceOf[LocalSearchScope])
      return null

    val sharedModuleScopes = sharedModules.map(m => GlobalSearchScope.moduleScope(m, true))
    GlobalSearchScope.union(sharedModuleScopes)
  }
}
