package org.jetbrains.sbt.execution

import consulo.annotation.component.ExtensionImpl
import consulo.module.Module
import consulo.module.content.layer.OrderEnumerationPolicy
import consulo.project.Project
import org.jetbrains.sbt.SbtSourceSetUtil.SbtSourceSetModuleExt
import org.jetbrains.sbt.SbtUtil

// upstream: SbtOrderEnumeratorHandler(Factory). Consulo policies have no module parameter, so the recursive and
// the non-recursive handler are two policies selected by isApplicable(Module). The two-level limit of recursive
// processing for grouping modules (shouldAddDependency) has no counterpart - it was only a performance optimization.
abstract class SbtOrderEnumerationPolicyBase(processDependenciesRecursively: Boolean) extends OrderEnumerationPolicy {

  override def isApplicable(project: Project): Boolean =
    SbtUtil.isSbtProject(project)

  override def isApplicable(module: Module): Boolean =
    SbtUtil.isSbtModule(module) && SbtOrderEnumerationPolicyBase.recursiveRequired(module) == processDependenciesRecursively

  //TODO SCL-22835
  override def shouldAddRuntimeDependenciesToTestCompilationClasspath: Boolean =
    true

  override def shouldProcessDependenciesRecursively: Boolean =
    processDependenciesRecursively
}

object SbtOrderEnumerationPolicyBase {

  private def recursiveRequired(module: Module): Boolean =
    SbtUtil.hasScalaCompilerSeparateProdTestSourcesEnabled(module.getProject) && !module.isSbtSourceSetModule
}

@ExtensionImpl
final class SbtOrderEnumerationPolicy extends SbtOrderEnumerationPolicyBase(false)

@ExtensionImpl
final class SbtRecursiveOrderEnumerationPolicy extends SbtOrderEnumerationPolicyBase(true)
