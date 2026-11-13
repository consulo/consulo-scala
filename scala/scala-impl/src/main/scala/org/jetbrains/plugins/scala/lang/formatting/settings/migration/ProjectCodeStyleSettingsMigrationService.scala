package org.jetbrains.plugins.scala.lang.formatting.settings.migration

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.component.persist.{State, Storage, StoragePathMacros}
import consulo.project.Project
import consulo.language.codeStyle.CodeStyleSettingsManager
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.formatting.settings.migration.CodeStyleSettingsMigrationServiceBase.MigrationItem

@State(
  name = "ProjectCodeStyleSettingsMigration",
  storages = Array[Storage](new Storage(StoragePathMacros.WORKSPACE_FILE))
)
// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class ProjectCodeStyleSettingsMigrationService(private val project: Project)
  extends CodeStyleSettingsMigrationServiceBase  {

  def init(): Unit = {
    migrateIfNeeded()

    //application-level code style settings should be migrated at some point in time
    //due to ApplicationComponent is deprecated this will be done on first project opening
    ApplicationCodeStyleSettingsMigrationService.instance.migrateIfNeeded()
  }

  override protected def migrate(migrations: Seq[MigrationItem], currentVersion: Int, latestVersion: Int): Unit = {
    // upstream: via CodeStyleSchemesModel(project).getProjectScheme
    val manager = CodeStyleSettingsManager.getInstance(project)
    val projectSettings = manager.PER_PROJECT_SETTINGS
    if (projectSettings != null) {
      val scalaSettings = projectSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])

      migrations.foreach(_.migrate(scalaSettings))

      Log.info(s"Migrated project-level scala code style settings from version $currentVersion to $latestVersion")

      manager.notifyCodeStyleSettingsChanged()
    }
  }
}
