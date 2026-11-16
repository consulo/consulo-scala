package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.application.WriteAction
import consulo.module.content.ModuleRootManager
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity
import org.jetbrains.plugins.scala.compiler.data.CompileOrder
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.extension.ScalaMutableModuleExtension

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*

/**
 * Enables Scala in the modules of Bazel Scala targets and applies their scalac options. The Scala SDK library of the
 * modules is set up during the sync by [[BazelScalaSdkExtension]].
 */
@ExtensionImpl
final class BazelScalaModuleConfiguratorPostSyncHook extends ProjectPostSyncHook {

  override def isEnabled(project: Project): Boolean = org.jetbrains.bazel.config.project.isBazelProject(project)

  override def onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment): CompletableFuture[Void] = {
    val project = environment.project
    val scalaModules = ModuleManager.getInstance(project).getModules.toSeq.flatMap { module =>
      Option(ScalaAddendumEntity.getScalaAddendumEntity(module)).map(module -> _)
    }
    if (scalaModules.isEmpty) return CompletableFuture.completedFuture(null)

    project.getUIAccess.giveAsync((() => {
      WriteAction.run[RuntimeException] { () =>
        scalaModules.foreach { case (module, _) => enableScala(module) }
      }
      scalaModules.foreach { case (module, scalaAddendum) =>
        module.configureScalaCompilerSettingsFrom("bazel", scalaAddendum.scalacOptions.asScala.toSeq, project, CompileOrder.Mixed)
      }
    }): Runnable).thenApply(_ => null)
  }

  private def enableScala(module: Module): Unit = {
    val model = ModuleRootManager.getInstance(module).getModifiableModel
    val scalaExtension = model.getExtensionWithoutCheck(classOf[ScalaMutableModuleExtension])
    if (scalaExtension != null && !scalaExtension.isEnabled) {
      scalaExtension.setEnabled(true)
      model.commit()
    }
    else model.dispose()
  }
}
