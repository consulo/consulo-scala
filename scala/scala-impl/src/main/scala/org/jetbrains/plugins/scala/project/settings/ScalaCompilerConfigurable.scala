package org.jetbrains.plugins.scala
package project
package settings

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.configurable.Configurable.Composite
import consulo.configurable.{Configurable, ProjectConfigurable, SearchableConfigurable, StandardConfigurableIds}
import jakarta.inject.Inject
import consulo.project.Project
import org.jetbrains.plugins.scala.compiler.data.ScalaCompilerSettingsState
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider
import org.jetbrains.plugins.scala.statistics.ScalaActionUsagesCollector

import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.Component
import consulo.ui.annotation.RequiredUIAccess

/**
 * @see [[org.jetbrains.plugins.scala.compiler.data.ScalaCompilerSettingsState]]
 */
@ExtensionImpl(id = "scala.compiler")
class ScalaCompilerConfigurable @Inject()(project: Project)
  extends SearchableConfigurable
    with ProjectConfigurable
    with Configurable.NoScroll
    with Composite {

  override def getId: String = ScalaCompilerConfigurable.Id

  override def getParentId: String = StandardConfigurableIds.COMPILER_GROUP

  //NOTE: duplicated in XML file
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("displayname.scala.compiler"))

  private var form: ScalaCompilerConfigurationPanel = scala.compiletime.uninitialized

  private def configuration = ScalaCompilerConfiguration.instanceIn(project)

  private def profilesPanel: ScalaCompilerProfilesPanel = form.getProfilesPanel

  @RequiredUIAccess
  override def createUIComponent(parentDisposable: Disposable): Component = {
    if (form == null) {
      form = new ScalaCompilerConfigurationPanel(project)
    }
    form.getContentPanel
  }

  @RequiredUIAccess
  override def disposeUIResources(): Unit = {
    form = null
  }

  override def isModified: Boolean = {
    if (form == null)
      return false
    if (form.getIncrementalityType != configuration.incrementalityType)
      return true
    if (!equalSettings(profilesPanel.getDefaultProfile, configuration.defaultProfile))
      return true
    if (!profilesPanel.getModuleProfiles.corresponds(configuration.customProfiles)(equalSettings))
      return true

    false
  }

  private def equalSettings(profile1: ScalaCompilerSettingsProfile, profile2: ScalaCompilerSettingsProfile): Boolean =
    settingsState(profile1) == settingsState(profile2)

  private def settingsState(profile: ScalaCompilerSettingsProfile): ScalaCompilerSettingsState =
    profile.getSettings.toState

  @RequiredUIAccess
  override def reset(): Unit = {
    if (form == null)
      return
    form.setIncrementalityType(configuration.incrementalityType)
    profilesPanel.initProfiles(configuration.defaultProfile, configuration.customProfiles)
  }

  @RequiredUIAccess
  override def apply(): Unit = {
    if (form == null)
      return
    val newIncType = form.getIncrementalityType
    if (newIncType != configuration.incrementalityType) {
      ScalaActionUsagesCollector.logIncrementalityTypeSet(newIncType, project)
    }

    configuration.incrementalityType = newIncType
    configuration.defaultProfile = profilesPanel.getDefaultProfile.copy
    configuration.customProfiles = profilesPanel.getModuleProfiles.map(_.copy)
    if (!project.isDefault) {
      DaemonCodeAnalyzer.getInstance(project).restart("Restart after ScalaCompilerConfigurable was applied")
      // upstream: BuildManager.getInstance().clearState(project), Consulo has no external build process state
    }
  }

  override def getConfigurables: Array[Configurable] = Array()

  override def getHelpTopic: String =
    ScalaWebHelpProvider.HelpPrefix + "compile-and-build-scala-projects.html"
}

object ScalaCompilerConfigurable {
  //NOTE: duplicated in XML file
  val Id = "scala.compiler"
}
