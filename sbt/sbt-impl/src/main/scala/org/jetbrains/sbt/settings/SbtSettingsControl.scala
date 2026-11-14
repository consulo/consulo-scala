package org.jetbrains.sbt.settings

import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.content.bundle.{Sdk, SdkTable}
import consulo.disposer.Disposable
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurable
import consulo.fileChooser.{FileChooserDescriptor, FileChooserDescriptorFactory, FileChooserTextBoxBuilder}
import consulo.localize.LocalizeValue
import consulo.module.ui.{BundleBox, BundleBoxBuilder}
import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{HorizontalLayout, VerticalLayout}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, Component, RadioGroup, TextBox}
import consulo.util.io.FileUtil
import org.jetbrains.sbt.SbtBundle

/**
 * General sbt settings (VM, launcher, sbt options, environment).
 */
// upstream: Swing SbtSettingsPane form (JrePathEditor, DistributionComboBox) - here a JDK box (project JDK / a registered
// JDK / a custom path) and a bundled / custom launcher choice
class SbtSettingsControl(settings: SbtSettings) extends ExternalSystemSettingsConfigurable[SbtSettings](settings) {

  import SbtSettingsControl.*

  private var jreBox: BundleBox = _
  private var customVMPath: FileChooserTextBoxBuilder.Controller = _
  private var launcherKind: RadioGroup[LauncherKind] = _
  private var maximumHeapSize: TextBox = _
  private var vmParameters: TextBox = _
  private var sbtOptions: TextBox = _
  private var launcherPath: FileChooserTextBoxBuilder.Controller = _
  private var sbtEnvironment: EnvironmentVariablesTextFieldWithBrowseButton = _
  private var useTerminalShell: CheckBox = _

  @RequiredUIAccess
  override def createUIComponent(uiDisposable: Disposable): Component = {
    jreBox = BundleBoxBuilder.create(uiDisposable)
      .withSdkTypeFilterByClass(classOf[JavaSdkType])
      .withNoneItem(LocalizeValue.localizeTODO("Project JDK"))
      .build()
    jreBox.addCustomBundleItem(CustomJreKey, LocalizeValue.localizeTODO("Custom JRE path").get, PlatformIconGroup.nodesFolderopened())
    customVMPath = FileChooserTextBoxBuilder.create(settings.getProject)
      .uiDisposable(uiDisposable)
      .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
      .build()
    jreBox.getComponent.addValueListener(_ => refreshEnabled())
    maximumHeapSize = TextBox.create()
    vmParameters = TextBox.create()
    sbtOptions = TextBox.create()
    launcherPath = FileChooserTextBoxBuilder.create(settings.getProject)
      .uiDisposable(uiDisposable)
      .dialogTitle(LocalizeValue.of(SbtBundle.message("sbt.settings.sbt.launcher.custom")))
      .fileChooserDescriptor(new FileChooserDescriptor(true, false, true, true, false, false).withFileFilter(_.getName.endsWith(".jar")))
      .build()
    launcherKind = RadioGroup.create()
    val launcherKindLayout = HorizontalLayout.create()
    launcherKindLayout.add(launcherKind.newButton(LocalizeValue.localizeTODO("Bundled"), LauncherKind.Bundled))
    launcherKindLayout.add(launcherKind.newButton(LocalizeValue.localizeTODO("Custom"), LauncherKind.Custom))
    launcherKind.addValueListener(_ => refreshEnabled())
    sbtEnvironment = new EnvironmentVariablesTextFieldWithBrowseButton()
    sbtOptions.setToolTipText(LocalizeValue.of(SbtBundle.message("sbt.settings.sbtOptions.tooltip")))
    useTerminalShell = CheckBox.create(LocalizeValue.localizeTODO("Use terminal for the sbt shell (sbt 1.4+)"))
    useTerminalShell.setToolTipText(LocalizeValue.localizeTODO(
      "The sbt shell runs in a terminal (sbt's own shell with JLine completion) instead of the console with an input field. Applies to new sbt shell instances."
    ))

    reset()

    val layout = VerticalLayout.create()
    layout.add(LabeledBuilder.filled(LocalizeValue.localizeTODO("JRE:"), jreBox))
    layout.add(LabeledBuilder.filled(LocalizeValue.localizeTODO("JRE path:"), customVMPath))
    layout.add(LabeledBuilder.filled(label("sbt.settings.maxHeapSize"), maximumHeapSize))
    layout.add(LabeledBuilder.filled(label("sbt.settings.vmParams"), vmParameters))
    layout.add(LabeledBuilder.filled(label("sbt.settings.sbtOptions"), sbtOptions))
    layout.add(LabeledBuilder.filled(label("sbt.settings.env.variables"), sbtEnvironment.getComponent))
    layout.add(LabeledBuilder.filled(label("sbt.settings.sbtLauncher"), launcherKindLayout))
    layout.add(LabeledBuilder.filled(LocalizeValue.localizeTODO("Launcher path:"), launcherPath))
    layout.add(useTerminalShell)
    layout
  }

  @RequiredUIAccess
  private def refreshEnabled(): Unit = {
    customVMPath.getComponent.setEnabled(jreBox.getSelectedBundleName == CustomJreKey)
    launcherPath.getComponent.setEnabled(launcherKind.getValue == LauncherKind.Custom)
  }

  private def selectedVMPath: String =
    jreBox.getSelectedBundleName match {
      case null => null
      case CustomJreKey => nonEmpty(customVMPath.getValue)
      case name => Option(SdkTable.getInstance().findSdk(name)).flatMap(sdk => Option(sdk.getHomePath)).orNull
    }

  private def selectedLauncherPath: String =
    if (launcherKind.getValue == LauncherKind.Custom) nonEmpty(launcherPath.getValue) else null

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
      selectedLauncherPath == settings.customLauncherPath &&
        heapSize == settings.maximumHeapSize &&
        Option(vmParameters.getValue).getOrElse("") == settings.vmParameters &&
        Option(sbtOptions.getValue).getOrElse("") == settings.sbtOptions &&
        selectedVMPath == settings.customVMPath &&
        sbtEnvironment.getEnvs == settings.sbtEnvironment &&
        sbtEnvironment.isPassParentEnvs == settings.sbtPassParentEnvironment &&
        useTerminalShell.getValue.booleanValue == settings.useTerminalShell
    }

  @RequiredUIAccess
  override def apply(): Unit =
    if (customVMPath != null) {
      settings.customLauncherPath = selectedLauncherPath
      settings.maximumHeapSize = heapSize
      settings.vmParameters = Option(vmParameters.getValue).getOrElse("")
      settings.sbtOptions = Option(sbtOptions.getValue).getOrElse("")
      settings.customVMPath = selectedVMPath
      settings.sbtEnvironment = sbtEnvironment.getEnvs
      settings.sbtPassParentEnvironment = sbtEnvironment.isPassParentEnvs
      settings.useTerminalShell = useTerminalShell.getValue
    }

  @RequiredUIAccess
  override def reset(): Unit =
    if (customVMPath != null) {
      launcherPath.setValue(Option(settings.customLauncherPath).getOrElse(""))
      launcherKind.setValue(if (settings.customLauncherPath == null) LauncherKind.Bundled else LauncherKind.Custom)
      maximumHeapSize.setValue(Option(settings.maximumHeapSize).map(_.toString).getOrElse(""))
      vmParameters.setValue(settings.vmParameters)
      sbtOptions.setValue(settings.sbtOptions)
      customVMPath.setValue(Option(settings.customVMPath).getOrElse(""))
      Option(settings.customVMPath) match {
        case None => jreBox.setSelectedNoneBundle()
        case Some(path) => jreBox.setSelectedBundle(findJdkByHome(path).map(_.getName).getOrElse(CustomJreKey))
      }
      sbtEnvironment.setEnvs(settings.sbtEnvironment)
      sbtEnvironment.setPassParentEnvs(settings.sbtPassParentEnvironment)
      useTerminalShell.setValue(settings.useTerminalShell)
      refreshEnabled()
    }
}

object SbtSettingsControl {

  private val CustomJreKey = "sbt.custom.jre"

  private enum LauncherKind {
    case Bundled, Custom
  }

  private def findJdkByHome(path: String): Option[Sdk] =
    SdkTable.getInstance().getAllSdks.find { sdk =>
      sdk.getSdkType.isInstanceOf[JavaSdkType] && sdk.getHomePath != null && FileUtil.pathsEqual(sdk.getHomePath, path)
    }
}
