package org.jetbrains.sbt
package project.settings

import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.configurable.ConfigurationException
import consulo.disposer.Disposable
import consulo.externalSystem.service.setting.{AbstractExternalProjectSettingsConfigurable, ExternalSystemSettingsPlace}
import consulo.localize.LocalizeValue
import consulo.module.ui.{BundleBox, BundleBoxBuilder}
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{LabeledLayout, VerticalLayout}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, Component, Label}
import org.jetbrains.annotations.Nls

/**
 * The settings UI is used it two places with slightly different UI:
 *  1. In `Settings | Build, Execution, Deployment | Build Tools | sbt` in `sbt Projects` subsection
 *  1. During new project creation via `Import project from existing sources` (`File | New | Project from Existing Sources...`)
 */
// upstream: AbstractExternalProjectSettingsControl with the Swing panel SbtExtraControls; the project reload after changing
// "use separate compiler output paths" is done by SbtExternalSystemConfigurable (the Consulo settings control has no project)
class SbtProjectSettingsControl(context: Context, settings: SbtProjectSettings)
  extends AbstractExternalProjectSettingsConfigurable[SbtProjectSettings](settings, SbtProjectSettingsControl.placeOf(context)) {

  private var jdkBox: Option[BundleBox] = None

  private var resolveClassifiersCheckBox: CheckBox = scala.compiletime.uninitialized
  private var resolveSbtClassifiersCheckBox: CheckBox = scala.compiletime.uninitialized
  private var useSbtShellForImportCheckBox: CheckBox = scala.compiletime.uninitialized
  private var useSbtShellForBuildCheckBox: CheckBox = scala.compiletime.uninitialized
  private var remoteDebugSbtShellCheckBox: CheckBox = scala.compiletime.uninitialized
  private var scalaVersionPreferenceCheckBox: CheckBox = scala.compiletime.uninitialized
  private var useSeparateCompilerOutputPaths: CheckBox = scala.compiletime.uninitialized
  private var generateManagedSourcesDuringProjectSync: CheckBox = scala.compiletime.uninitialized
  private var useSeparateCompilerOutputPathsWarning: Label = scala.compiletime.uninitialized

  @RequiredUIAccess
  override protected def createExtraUIComponent(uiDisposable: Disposable): Component = {
    val settings = getSettings

    resolveClassifiersCheckBox = checkBox("sbt.settings.resolveClassifiers", settings.resolveClassifiers)
    resolveSbtClassifiersCheckBox = checkBox("sbt.settings.resolveSbtClassifiers", settings.resolveSbtClassifiers)
    useSbtShellForImportCheckBox = checkBox("sbt.settings.useShellForImport", settings.importWithShell)
    useSbtShellForBuildCheckBox = checkBox("sbt.settings.useShellForBuild", settings.buildWithShell)
    remoteDebugSbtShellCheckBox = checkBox("sbt.settings.remoteDebug", settings.enableDebugSbtShell)
    scalaVersionPreferenceCheckBox = checkBox("sbt.settings.scalaVersionPreference", settings.preferScala2)
    useSeparateCompilerOutputPaths = checkBox("use.separate.compiler.output.paths", settings.useSeparateCompilerOutputPaths)
    generateManagedSourcesDuringProjectSync = checkBox(
      "generate.managed.sources.during.project.sync.label",
      settings.generateManagedSourcesDuringProjectSync,
      "generate.managed.sources.during.project.sync.tooltip"
    )
    useSeparateCompilerOutputPathsWarning = Label.create(LocalizeValue.of(SbtBundle.message("use.separate.compiler.output.paths.warning")))

    useSeparateCompilerOutputPaths.addValueListener(_ => refreshOutputPathsWarning())
    useSbtShellForBuildCheckBox.addValueListener(_ => refreshOutputPathsWarning())
    refreshOutputPathsWarning()

    val download = VerticalLayout.create()
    download.add(resolveClassifiersCheckBox)
    download.add(resolveSbtClassifiersCheckBox)

    val useShell = VerticalLayout.create()
    useShell.add(useSbtShellForImportCheckBox)
    useShell.add(useSbtShellForBuildCheckBox)

    val shell = VerticalLayout.create()
    shell.add(LabeledBuilder.simple(LocalizeValue.of(SbtBundle.message("sbt.settings.useShell")), useShell))
    shell.add(remoteDebugSbtShellCheckBox)

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.simple(LocalizeValue.of(SbtBundle.message("sbt.settings.download")), download))
    layout.add(scalaVersionPreferenceCheckBox)
    layout.add(useSeparateCompilerOutputPaths)
    layout.add(generateManagedSourcesDuringProjectSync)
    layout.add(useSeparateCompilerOutputPathsWarning)
    layout.add(LabeledLayout.create(LocalizeValue.of(SbtBundle.message("sbt.settings.shell.title")), shell))

    if (context == Context.Wizard) {
      val box = BundleBoxBuilder.create(uiDisposable)
        .withSdkTypeFilterByClass(classOf[JavaSdkType])
        .build()
      settings.jdkName.foreach(box.setSelectedBundle)
      jdkBox = Some(box)
      layout.add(LabeledBuilder.sided(LocalizeValue.of(SbtBundle.message("sbt.settings.project.jdk")), box))

      remoteDebugSbtShellCheckBox.setVisible(false)
    }

    layout
  }

  @RequiredUIAccess
  override protected def isExtraModified: Boolean = {
    val settings = getSettings

    selectedJdkName != settings.jdkName ||
      resolveClassifiersCheckBox.getValue.booleanValue != settings.resolveClassifiers ||
      resolveSbtClassifiersCheckBox.getValue.booleanValue != settings.resolveSbtClassifiers ||
      useSbtShellForImportCheckBox.getValue.booleanValue != settings.useSbtShellForImport ||
      useSbtShellForBuildCheckBox.getValue.booleanValue != settings.useSbtShellForBuild ||
      remoteDebugSbtShellCheckBox.getValue.booleanValue != settings.enableDebugSbtShell ||
      scalaVersionPreferenceCheckBox.getValue.booleanValue != settings.preferScala2 ||
      useSeparateCompilerOutputPaths.getValue.booleanValue != settings.useSeparateCompilerOutputPaths ||
      generateManagedSourcesDuringProjectSync.getValue.booleanValue != settings.generateManagedSourcesDuringProjectSync
  }

  @RequiredUIAccess
  override protected def applyExtra(): Unit = {
    val settings = getSettings

    if (jdkBox.isDefined && selectedJdkName.isEmpty)
      throw new ConfigurationException(LocalizeValue.of(SbtBundle.message("sbt.settings.project.jdk")))

    if (jdkBox.isDefined) settings.jdk = selectedJdkName.orNull
    settings.resolveClassifiers = resolveClassifiersCheckBox.getValue
    settings.resolveSbtClassifiers = resolveSbtClassifiersCheckBox.getValue
    settings.useSbtShellForImport = useSbtShellForImportCheckBox.getValue
    settings.useSbtShellForBuild = useSbtShellForBuildCheckBox.getValue
    settings.enableDebugSbtShell = remoteDebugSbtShellCheckBox.getValue
    settings.preferScala2 = scalaVersionPreferenceCheckBox.getValue
    settings.useSeparateCompilerOutputPaths = useSeparateCompilerOutputPaths.getValue
    settings.generateManagedSourcesDuringProjectSync = generateManagedSourcesDuringProjectSync.getValue
  }

  private def selectedJdkName: Option[String] =
    jdkBox match {
      case Some(box) => Option(box.getSelectedBundleName)
      case None => getSettings.jdkName
    }

  @RequiredUIAccess
  private def refreshOutputPathsWarning(): Unit =
    useSeparateCompilerOutputPathsWarning.setVisible(
      useSeparateCompilerOutputPaths.getValue.booleanValue && useSbtShellForBuildCheckBox.getValue.booleanValue
    )

  @RequiredUIAccess
  private def checkBox(labelKey: String, value: Boolean, tooltipKey: String = null): CheckBox = {
    @Nls val label = SbtBundle.message(labelKey)
    val box = CheckBox.create(LocalizeValue.of(label), value)
    box.setToolTipText(LocalizeValue.of(SbtBundle.message(Option(tooltipKey).getOrElse(labelKey + ".tooltip"))))
    box
  }
}

object SbtProjectSettingsControl {
  private def placeOf(context: Context): ExternalSystemSettingsPlace = context match {
    case Context.Wizard => ExternalSystemSettingsPlace.IMPORT
    case Context.Configuration => ExternalSystemSettingsPlace.SETTINGS
  }
}
