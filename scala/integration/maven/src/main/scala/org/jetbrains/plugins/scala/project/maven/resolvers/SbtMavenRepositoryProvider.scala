package org.jetbrains.plugins.scala.project.maven.resolvers

import consulo.annotation.component.ExtensionImpl
import consulo.maven.rt.server.common.model.MavenRemoteRepository
import consulo.module.ModuleManager
import consulo.project.Project
import org.jetbrains.idea.maven.indices.{MavenIndex, MavenRepositoryProvider}
import org.jetbrains.sbt.project.module.SbtModule
import org.jetbrains.sbt.api.resolvers.{SbtMavenResolver, SbtResolver}

import scala.jdk.CollectionConverters.*

@ExtensionImpl
final class SbtMavenRepositoryProvider extends MavenRepositoryProvider:
  override def getRemoteRepositories(project: Project): java.util.Set[MavenRemoteRepository] =
    projectResolvers(project).collect:
      case r: SbtMavenResolver =>
        MavenRemoteRepository(
          /*              id = */ r.name,
          /*            name = */ null,
          /*             url = */ MavenIndex.normalizePathOrUrl(r.root),
          /*          layout = */ null,
          /*  releasesPolicy = */ null,
          /* snapshotsPolicy = */ null
        )
    .asJava

  private def projectResolvers(project: Project): Set[SbtResolver] =
    ModuleManager.getInstance(project).getModules.toSet.flatMap(SbtModule.Resolvers.apply)
