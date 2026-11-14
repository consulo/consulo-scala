package org.jetbrains.sbt.settings

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.{ProjectConfigurable, StandardConfigurableIds}
import consulo.externalSystem.importing.ImportSpecBuilder
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher
import consulo.externalSystem.service.setting.AbstractExternalSystemConfigurable
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import jakarta.inject.Inject
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.{SbtProjectSettings, SbtProjectSettingsListener}

import scala.jdk.CollectionConverters.*

// upstream: the project/system settings controls are created by SbtSettingsConfigurableFactory;
// the TraverseUIMode linking of a default project setting for the options index is replaced by SbtSearchableOptionContributor
@ExtensionImpl
class SbtExternalSystemConfigurable @Inject()(project: Project)
  extends AbstractExternalSystemConfigurable[SbtProjectSettings, SbtProjectSettingsListener, SbtSettings](project, SbtProjectSystem.Id)
    with ProjectConfigurable {

  override def getId: String = "sbt.project.settings.configurable"

  override def getParentId: String = StandardConfigurableIds.EXECUTION_GROUP

  // upstream: SbtProjectSettingsControl.reloadProjectIfNeeded - the settings control has no project in Consulo
  @RequiredUIAccess
  override def apply(): Unit = {
    val before = separateCompilerOutputPaths
    super.apply()
    val changed = separateCompilerOutputPaths.exists { case (path, value) => before.get(path).exists(_ != value) }
    if (changed) {
      ExternalSystemProjectRefresher.getInstance()
        .refreshProjects(new ImportSpecBuilder(project, SbtProjectSystem.Id).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC))
    }
  }

  private def separateCompilerOutputPaths: Map[String, Boolean] =
    SbtSettings.getInstance(project).getLinkedProjectsSettings.asScala
      .map(settings => settings.getExternalProjectPath -> settings.useSeparateCompilerOutputPaths)
      .toMap
}
