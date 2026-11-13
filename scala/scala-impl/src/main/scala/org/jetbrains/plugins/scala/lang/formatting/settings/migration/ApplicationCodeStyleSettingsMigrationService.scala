package org.jetbrains.plugins.scala.lang.formatting.settings.migration

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton

import consulo.application.ApplicationManager
import consulo.component.persist.{State, Storage}
import consulo.language.codeStyle.{CodeStyleScheme, CodeStyleSchemes}
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.formatting.settings.migration.CodeStyleSettingsMigrationServiceBase.MigrationItem
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

@State(
  name = "ApplicationCodeStyleSettingsMigration",
  storages = Array[Storage](new Storage(ScalaApplicationSettings.STORAGE_FILE_NAME))
)
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
class ApplicationCodeStyleSettingsMigrationService extends CodeStyleSettingsMigrationServiceBase {

  override protected def migrate(migrations: Seq[MigrationItem], currentVersion: Int, latestVersion: Int): Unit = {
    val appLevelSchemes: Seq[CodeStyleScheme] = CodeStyleSchemes.getInstance.getSchemes.toSeq

    appLevelSchemes.zipWithIndex.foreach { case (scheme, idx) =>
      val scalaSettings = scheme.getCodeStyleSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])
      migrations.foreach(_.migrate(scalaSettings))
      Log.info(s"Migrated installation-level scala code style settings ($idx) from version $currentVersion to $latestVersion")
    }
  }
}

object ApplicationCodeStyleSettingsMigrationService {

  def instance: ApplicationCodeStyleSettingsMigrationService =
    ApplicationManager.getApplication.getInstance(classOf[ApplicationCodeStyleSettingsMigrationService])
}
