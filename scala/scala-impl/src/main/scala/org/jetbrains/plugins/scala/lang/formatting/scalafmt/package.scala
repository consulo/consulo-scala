package org.jetbrains.plugins.scala.lang.formatting

import consulo.module.ModuleManager
import consulo.project.Project
import org.jetbrains.plugins.scala.DependencyManagerBase
import org.jetbrains.plugins.scala.DependencyManagerBase.{IvyResolver, MavenResolver}
import org.jetbrains.sbt.project.module.SbtModule
import org.jetbrains.sbt.resolvers.{SbtIvyResolver, SbtMavenResolver}

package object scalafmt {

  private[scalafmt] def projectResolvers(project: Project): Seq[DependencyManagerBase.Resolver] = {
    val resolversSbt = resolversFromSbt(project)
    val resolvers = if (resolversSbt.isEmpty) // imply that it's a non-sbt project
      resolversFromRemoteJarRepositories(project)
    else
      resolversSbt
    resolvers
  }

  private def resolversFromSbt(project: Project): Seq[DependencyManagerBase.Resolver] = {
    val modules = ModuleManager.getInstance(project).getModules.toSet
    val moduleSbtResolvers = modules.flatMap(SbtModule.Resolvers.apply)
    moduleSbtResolvers.toSeq.collect {
      case r: SbtMavenResolver => MavenResolver(r.name, ensureIsURL(r.root))
      case r: SbtIvyResolver if !r.isLocal => IvyResolver(r.name, ensureIsURL(r.root))
    }
  }

  // upstream: resolvers from `Remote Jar Repositories` (com.intellij.jarRepository.RemoteRepositoriesConfiguration),
  // which Gradle and Maven projects import; no Consulo counterpart
  private def resolversFromRemoteJarRepositories(project: Project): Seq[DependencyManagerBase.MavenResolver] = Nil

  // e.g. `file://C:/temp/`
  private val ResourceWithProtocol = raw"^\w+://.*$$".r

  private def ensureIsURL(path: String): String = {
    val pathWithProtocol = if (ResourceWithProtocol.matches(path)) path else {
      // e.g. "local" ivy resolver contains file path, not URL
      // so we need to add the protocol and escape `\` (on windows)
      "file://" + path.replace('\\', '/')
    }
    pathWithProtocol
  }
}
