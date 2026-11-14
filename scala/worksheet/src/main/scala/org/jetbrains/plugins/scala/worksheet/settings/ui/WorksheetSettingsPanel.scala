package org.jetbrains.plugins.scala.worksheet.settings.ui

import consulo.localize.LocalizeValue
import consulo.module.Module
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{HorizontalLayout, LabeledLayout, VerticalLayout}
import consulo.ui.model.MutableFlatDataModel
import consulo.ui.model.FlatDataModel
import consulo.ui.{CheckBox, ComboBox, Component}
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.worksheet.settings.ui.WorksheetSettingsPanel.TabTypeData
import org.jetbrains.plugins.scala.worksheet.settings.ui.WorksheetSettingsPanel.TabTypeData._
import org.jetbrains.plugins.scala.worksheet.settings.{WorksheetExternalRunType, WorksheetModuleUtil}
import org.jetbrains.plugins.scala.worksheet.{WorksheetBundle, WorksheetUtils}

import scala.jdk.CollectionConverters._

/** upstream: a Swing panel with MigLayout, here built with the unified UI */
private final class WorksheetSettingsPanel(
  tabTypeData: TabTypeData,
  settingsData: WorksheetSettingsData,
  availableProfilesProvider: () => Seq[String]
) {
  import WorksheetSettingsPanel._

  private def message(key: String, params: Any*): LocalizeValue =
    LocalizeValue.of(WorksheetBundle.message(key, params*))

  private val interactiveModeCheckBox      = CheckBox.create(message("worksheet.settings.panel.interactive.mode"))
  private val makeProjectBeforeRunCheckBox = CheckBox.create(message("worksheet.settings.panel.change.make.button"))
  private val runTypeComboBox              = ComboBox.create[WorksheetExternalRunType](WorksheetExternalRunType.getAllRunTypes*)

  // upstream: ModulesComboBox, an empty selection is allowed in the default project settings tab
  private val moduleComboBox: ComboBox[ModuleItem] = {
    val modules = WorksheetModuleUtil.allModulesWithScalaSdk(project).map(module => Some(module): ModuleItem)
    val items = if (tabTypeData.is[DefaultProjectSettingsTab]) None +: modules else modules
    ComboBox.create[ModuleItem](items.asJava)
  }

  private val compilerProfilesModel: MutableFlatDataModel[String] = FlatDataModel.of(java.util.List.of[String]())
  private val compilerProfileComboBox = ComboBox.create[String](compilerProfilesModel)

  private val openCompilerProfileSettingsButton = ShowCompilerProfileSettingsButton.create(
    project,
    () => selectedProfile,
    () => updateProfiles(selectedProfile, listOfProfiles)
  )

  val component: Component = createLayout()

  locally {
    initData(settingsData)
  }

  def filledSettingsData: WorksheetSettingsData =
    WorksheetSettingsData(
      interactiveModeCheckBox.getValue,
      makeProjectBeforeRunCheckBox.getValue,
      runTypeComboBox.getValue,
      Option(moduleComboBox.getValue).flatten.orNull,
      selectedProfile
    )

  private def selectedProfile: String =
    Option(compilerProfileComboBox.getValue).filterNot(_ == NoProfile).orNull

  private def project: Project =
    tabTypeData.project

  private def listOfProfiles: Seq[String] =
    availableProfilesProvider()

  @RequiredUIAccess
  private def initData(settingsData: WorksheetSettingsData): Unit = {
    // FIXME: Currently you can't unselect selected module, see: SCL-18054, IDEA-239791
    moduleComboBox.setTextRenderer { item =>
      Option(item).flatten match {
        case Some(module) => LocalizeValue.of(module.getName)
        case None         => message("worksheet.settings.panel.no.module")
      }
    }
    moduleComboBox.setValue(Option(settingsData.cpModule))

    val allowChangingModule = tabTypeData match {
      case DefaultProjectSettingsTab(_)          => true
      case FileSettingsTab(project, virtualFile) => WorksheetUtils.isScratchWorksheet(project, virtualFile)
    }
    moduleComboBox.setEnabled(allowChangingModule)

    tabTypeData match {
      case DefaultProjectSettingsTab(_) =>
        moduleComboBox.setToolTipText(message("worksheet.settings.panel.default.setting.is.only.used.in.scratch.files.note"))

      case FileSettingsTab(_, _) =>
        if (!allowChangingModule) {
          moduleComboBox.setToolTipText(message("worksheet.settings.panel.setting.can.be.changed.in.scratch.files.note"))
        }
    }

    runTypeComboBox.setTextRenderer(runType => if (runType == null) LocalizeValue.empty() else LocalizeValue.of(runType.getMenuText))
    runTypeComboBox.setValue(settingsData.runType)
    interactiveModeCheckBox.setValue(settingsData.isInteractive)
    makeProjectBeforeRunCheckBox.setValue(settingsData.isMakeBeforeRun)

    // custom renderer to allow showing unselected item
    compilerProfileComboBox.setTextRenderer { profile =>
      if (profile == null || profile == NoProfile) message("worksheet.settings.panel.no.profile.selected")
      else LocalizeValue.of(profile)
    }
    if (allowDeselectingProfile)
      compilerProfileComboBox.setToolTipText(message("worksheet.settings.panel.compiler.profile.of.worksheet.module.will.be.used.note"))
    updateProfiles(settingsData.compilerProfile, listOfProfiles)
  }

  @RequiredUIAccess
  private def updateProfiles(selectedProfile: String, profiles: Seq[String]): Unit = {
    val modelItems = if (allowDeselectingProfile) NoProfile +: profiles else profiles
    compilerProfilesModel.removeAll()
    modelItems.foreach(compilerProfilesModel.add)
    val selected = Option(selectedProfile).filter(modelItems.contains).getOrElse(if (allowDeselectingProfile) NoProfile else null)
    compilerProfileComboBox.setValue(selected)
  }

  private def allowDeselectingProfile: Boolean =
    tabTypeData.is[TabTypeData.DefaultProjectSettingsTab]

  @RequiredUIAccess
  private def createLayout(): Component = {
    val root = VerticalLayout.create()
    root.add(interactiveModeCheckBox)
    root.add(makeProjectBeforeRunCheckBox)
    root.add(LabeledLayout.create(message("worksheet.settings.panel.run.type"), runTypeComboBox))

    //TODO: handle the case when WS is not in the main/test module
    // also related to
    // https://youtrack.jetbrains.com/issue/SCL-24317/Cant-create-Scala-Worksheet-in-sbt-root-project-dir-due-to-missing-Scala-SDK-in-the-parent-module
    root.add(LabeledLayout.create(message("worksheet.settings.panel.use.class.path.of.module"), moduleComboBox))

    val profileRow = HorizontalLayout.create()
    profileRow.add(compilerProfileComboBox)
    profileRow.add(openCompilerProfileSettingsButton)
    root.add(LabeledLayout.create(message("worksheet.settings.panel.compiler.profile"), profileRow))
    root
  }
}

object WorksheetSettingsPanel {

  private type ModuleItem = Option[Module]

  // upstream: a null item of the combo box model
  private val NoProfile = ""

  sealed abstract class TabTypeData {
    def project: Project
  }
  object TabTypeData {
    final case class DefaultProjectSettingsTab(override val project: Project) extends TabTypeData
    final case class FileSettingsTab(override val project: Project, virtualFile: VirtualFile) extends TabTypeData
  }

  case class UiData(profiles: Seq[String])
}
