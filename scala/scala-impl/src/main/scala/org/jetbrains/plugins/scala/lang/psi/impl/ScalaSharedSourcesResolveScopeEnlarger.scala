package org.jetbrains.plugins.scala.lang.psi.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.util.ModuleUtilCore
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.ResolveScopeEnlarger
import consulo.language.psi.scope.GlobalSearchScope
import consulo.content.scope.SearchScope
import org.jetbrains.plugins.scala.project.ModuleExt

// upstream: @ApiStatus.Internal
@ExtensionImpl
final class ScalaSharedSourcesResolveScopeEnlarger extends ResolveScopeEnlarger {

  override def getAdditionalResolveScope(file: VirtualFile, project: Project): SearchScope = {
    val module = ModuleUtilCore.findModuleForFile(file, project)
    if (module == null)
      return null

    val representativeModule = module.findRepresentativeModuleForSharedSourceModule.orNull
    if (representativeModule == null)
      return null

    GlobalSearchScope.moduleWithDependenciesScope(representativeModule)
  }
}
