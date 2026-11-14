package org.jetbrains.sbt.project.structure.data

import consulo.platform.Platform
import org.jetbrains.sbt.project.structure.data.XmlDeserializer.deserializeOne

case class DependencyData(projects: Dependencies[ProjectDependencyData],
                          modules: Dependencies[ModuleDependencyData],
                          jars: Dependencies[JarDependencyData])

object DependencyData:
  given (PathConstructor[String], Platform) => XmlDeserializer[DependencyData] = what =>
    for
      projects <- (what \ "projects").deserializeOne[Dependencies[ProjectDependencyData]]
      modules <- (what \ "modules").deserializeOne[Dependencies[ModuleDependencyData]]
      jars <- (what \ "jars").deserializeOne[Dependencies[JarDependencyData]]
    yield DependencyData(projects, modules, jars)
