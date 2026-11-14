package org.jetbrains.sbt.settings

import consulo.disposer.Disposable
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurable
import consulo.fileChooser.{FileChooserDescriptor, FileChooserDescriptorFactory, FileChooserTextBoxBuilder}
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.VerticalLayout
import consulo.ui.util.LabeledBuilder
import consulo.ui.{Component, TextBox}
import org.jetbrains.sbt.SbtBundle

/**
 * General sbt settings (VM, launcher, sbt options, environment).
 */
// upstream: Swing SbtSettingsPane form (JrePathEditor, DistributionComboBox); the custom JRE and the custom launcher are
// plain path choosers (empty = project JDK / bundled launcher)
class SbtSettingsControl(settings: SbtSettings) extends ExternalSystemSettingsConfigurable[SbtSettings](settings) {

  private var customVMPath: FileChooserTextBoxBuilder.Controller = _
  private var maximumHeapSize: TextBox = _
  private var vmParameters: TextBox = _
  private var sbtOptions: TextBox = _
  private var launcherPath: FileChooserTextBoxBuilder.Controller = _
  private var sbtEnvironment: EnvironmentVariablesTextFieldWithBrowseButton = _

  @RequiredUIAccess
  override def createUIComponent(uiDisposable: Disposable): Component = {
    customVMPath = FileChooserTextBoxBuilder.create(settings.getProject)
      .uiDisposable(uiDisposable)
      .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
      .build()
    maximumHeapSize = TextBox.create()
    vmParameters = TextBox.create()
    sbtOptions = TextBox.create()
    launcherPath = FileChooserTextBoxBuilder.create(settings.getProject)
      .uiDisposable(uiDisposable)
      .dialogTitle(LocalizeValue.of(SbtBundle.message("sbt.settings.sbt.launcher.custom")))
      .fileChooserDescriptor(new FileChooserDescriptor(true, false, true, true, false, false).withFileFilter(_.getName.endsWith(".jar")))
      .build()
    sbtEnvironment = new EnvironmentVariablesTextFieldWithBrowseButton()
    sbtOptions.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.settings.sbtOptions.tooltip")))

    reset()

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.filled(LocalizeValue.localizeTODO("JRE:"), customVMPath))
    layout.add(LabeledBuilder.filled(label("sbt.settings.maxHeapSize"), maximumHeapSize))
    layout.add(LabeledBuilder.filled(label("sbt.settings.vmParams"), vmParameters))
    layout.add(LabeledBuilder.filled(label("sbt.settings.sbtOptions"), sbtOptions))
    layout.add(LabeledBuilder.filled(label("sbt.settings.env.variables"), sbtEnvironment.getComponent))
    layout.add(LabeledBuilder.filled(label("sbt.settings.sbtLauncher"), launcherPath))
    layout
  }

  private def label(key: String): LocalizeValue =
    LocalizeValue.of(SbtBundle.message(key).replace("&", ""))

  private def nonEmpty(text: String): String =
    Option(text).map(_.trim).filter(_.nonEmpty).orNull

  private def heapSize: Integer =
    nonEmpty(maximumHeapSize.getValue) match {
      case null => null
      case text => text.toIntOption.map(Integer.valueOf).orNull
    }

  @RequiredUIAccess
  override def isModified: Boolean =
    customVMPath != null && ! {
      nonEmpty(launcherPath.getValue) == settings.customLauncherPath &&
        heapSize == settings.maximumHeapSize &&
        Option(vmParameters.getValue).getOrElse("") == settings.vmParameters &&
        Option(sbtOptions.getValue).getOrElse("") == settings.sbtOptions &&
        nonEmpty(customVMPath.getValue) == settings.customVMPath &&
        sbtEnvironment.getEnvs == settings.sbtEnvironment &&
        sbtEnvironment.isPassParentEnvs == settings.sbtPassParentEnvironment
    }

  @RequiredUIAccess
  override def apply(): Unit =
    if (customVMPath != null) {
      settings.customLauncherPath = nonEmpty(launcherPath.getValue)
      settings.maximumHeapSize = heapSize
      settings.vmParameters = Option(vmParameters.getValue).getOrElse("")
      settings.sbtOptions = Option(sbtOptions.getValue).getOrElse("")
      settings.customVMPath = nonEmpty(customVMPath.getValue)
      settings.sbtEnvironment = sbtEnvironment.getEnvs
      settings.sbtPassParentEnvironment = sbtEnvironment.isPassParentEnvs
    }

  @RequiredUIAccess
  override def reset(): Unit =
    if (customVMPath != null) {
      launcherPath.setValue(Option(settings.customLauncherPath).getOrElse(""))
      maximumHeapSize.setValue(Option(settings.maximumHeapSize).map(_.toString).getOrElse(""))
      vmParameters.setValue(settings.vmParameters)
      sbtOptions.setValue(settings.sbtOptions)
      customVMPath.setValue(Option(settings.customVMPath).getOrElse(""))
      sbtEnvironment.setEnvs(settings.sbtEnvironment)
      sbtEnvironment.setPassParentEnvs(settings.sbtPassParentEnvironment)
    }
}
