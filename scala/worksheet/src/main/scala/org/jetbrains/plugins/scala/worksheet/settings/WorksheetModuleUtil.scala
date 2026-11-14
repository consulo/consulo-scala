package org.jetbrains.plugins.scala.worksheet.settings

import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}

private object WorksheetModuleUtil {
  def allModulesWithScalaSdk(project: Project): Seq[Module] = {
    val allScalaModules = project.modulesWithScala
    val modulesWithScalaLibrary = allScalaModules.filter { module =>
      val libraries = module.libraries
      libraries.exists { library =>
        val name = library.getName
        (name ne null) && (name.contains("scala-library") || name.contains("scala3-library"))
      }
    }
    // In case there are no modules left, just use the non-filtered list. Some tests also start to fail in this case.
    if (modulesWithScalaLibrary.nonEmpty) modulesWithScalaLibrary else allScalaModules
  }

  // upstream: only production modules for sbt projects imported with separate production and test modules
  // (the Consulo sbt import creates one module per directory)
  def allProductionModulesWithScalaSdk(project: Project): Seq[Module] =
    allModulesWithScalaSdk(project)

  // upstream: stale sbt source set modules (see allProductionModulesWithScalaSdk)
  def isStale(module: Module): Boolean = false
}
