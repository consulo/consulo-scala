package org.jetbrains.sbt.settings

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.component.persist.{PersistentStateComponent, RoamingType, State, Storage}
import consulo.disposer.Disposable
import consulo.externalSystem.setting.{AbstractExternalSystemSettings, ExternalSystemSettingsListener}
import consulo.externalSystem.util.{ExternalSystemApiUtil, ExternalSystemConstants}
import consulo.module.Module
import consulo.project.Project
import consulo.util.xml.serializer.annotation.AbstractCollection
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.annotations.{NotNull, Nullable}
import org.jetbrains.sbt.api.{JvmMemorySize, RichOption}
import org.jetbrains.sbt.project.settings.{SbtProjectSettings, SbtProjectSettingsListener, SbtProjectSettingsListenerAdapter}

import java.util as j
import scala.beans.BeanProperty

/**
 * Represents some configuration parameters which will be used to start sbt process during project reload and when launching sbt shell.
 * (such as VM path, VM options, environment variables, extra sbt options)
 *
 * These settings are displayed in `Settings | Build, Execution, Deployment | Build Tools | sbt`<br>
 * in `General Settings` subsection
 *
 * @see [[org.jetbrains.sbt.settings.SbtSettingsControl]]<br>
 *      [[org.jetbrains.sbt.settings.SbtSettingsPane]]<br>
 *      (UI for current settings)
 * @see [[org.jetbrains.sbt.project.settings.SbtProjectSettings]]
 */
//noinspection ConvertNullInitializerToUnderscore
// upstream: lives in sbt-api
@State(
  name = "ScalaSbtSettings",
  storages = Array[Storage](new Storage(value = "sbt.xml", roamingType = RoamingType.DISABLED))
)
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
final class SbtSettings @Inject()(project: Project)
  extends AbstractExternalSystemSettings[SbtSettings, SbtProjectSettings, SbtProjectSettingsListener](SbtSettings.SbtTopic, project)
  with PersistentStateComponent[SbtSettings.State] {

  @BeanProperty @Nullable var customLauncherPath: String = null
  @BeanProperty var maximumHeapSize: Integer = null
  @BeanProperty var vmParameters: String = ""
  @BeanProperty var sbtOptions: String = ""
  @BeanProperty @Nullable var customVMPath: String = null
  @BeanProperty @Nullable var customSbtStructurePath: String = null
  @BeanProperty var sbtEnvironment: j.Map[String, String] = j.Collections.emptyMap
  @BeanProperty var sbtPassParentEnvironment: Boolean = true

  override def getState: SbtSettings.State = {
    val state = new SbtSettings.State
    fillState(state)

    state.customLauncherPath = customLauncherPath
    state.maximumHeapSize = maximumHeapSize
    state.vmParameters = Option(vmParameters).getOrElse("")
    state.customVMPath = customVMPath
    state.customSbtStructurePath = customSbtStructurePath
    state.sbtOptions = Option(sbtOptions).getOrElse("")
    state.sbtEnvironment = sbtEnvironment
    state.sbtPassParentEnvironment = sbtPassParentEnvironment

    state
  }

  override def loadState(state: SbtSettings.State): Unit = {
    super[AbstractExternalSystemSettings].loadState(state)

    customLauncherPath = state.customLauncherPath
    maximumHeapSize = state.maximumHeapSize
    vmParameters = Option(state.vmParameters).getOrElse("")
    sbtOptions = Option(state.sbtOptions).getOrElse("")
    customVMPath = state.customVMPath
    customSbtStructurePath = state.customSbtStructurePath
    sbtEnvironment = state.sbtEnvironment
    sbtPassParentEnvironment = state.sbtPassParentEnvironment
  }

  override def subscribe(listener: ExternalSystemSettingsListener[SbtProjectSettings]): Unit =
    subscribe(listener, project)

  def subscribe(listener: ExternalSystemSettingsListener[SbtProjectSettings], parentDisposable: Disposable): Unit = {
    val adapter = new SbtProjectSettingsListenerAdapter(listener)
    val messageBus = getProject.getMessageBus
    messageBus.connect(parentDisposable).subscribe(SbtSettings.SbtTopic, adapter)
  }

  override def copyExtraSettingsFrom(settings: SbtSettings): Unit = {
    customLauncherPath = settings.customLauncherPath
    maximumHeapSize = settings.maximumHeapSize
    vmParameters = Option(settings.vmParameters).getOrElse("")
    sbtOptions = Option(settings.sbtOptions).getOrElse("")
    customVMPath = settings.customVMPath
    customSbtStructurePath = settings.customSbtStructurePath
    sbtEnvironment = settings.sbtEnvironment
    sbtPassParentEnvironment = settings.sbtPassParentEnvironment
  }

  def getLinkedProjectSettings(module: Module): Option[SbtProjectSettings] =
    Option(ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.ROOT_PROJECT_PATH_KEY))
      .safeMap(getLinkedProjectSettings)

  @Nullable
  override def getLinkedProjectSettings(linkedProjectPath: String): SbtProjectSettings =
    super.getLinkedProjectSettings(linkedProjectPath) match {
      case null => super.getLinkedProjectSettings(ExternalSystemApiUtil.normalizePath(linkedProjectPath))
      case settings => settings
    }

  override def checkSettings(old: SbtProjectSettings, current: SbtProjectSettings): Unit = {}
}

object SbtSettings {
  def getInstance(@NotNull project: Project): SbtSettings = project.getInstance(classOf[SbtSettings])

  val hiddenDefaultMaxHeapSize: JvmMemorySize = JvmMemorySize.Megabytes(1536)

  val SbtTopic: Class[SbtProjectSettingsListener] = classOf[SbtProjectSettingsListener]

  //noinspection ConvertNullInitializerToUnderscore
  class State extends AbstractExternalSystemSettings.State[SbtProjectSettings] {
    @BeanProperty @Nullable var customLauncherPath: String = null
    @BeanProperty @Nullable var maximumHeapSize: Integer = null
    @BeanProperty var vmParameters: String = ""
    @BeanProperty var sbtOptions: String = ""
    @BeanProperty @Nullable var customVMPath: String = null
    @BeanProperty @Nullable var customSbtStructurePath: String = null
    @BeanProperty var sbtEnvironment: j.Map[String, String] = j.Collections.emptyMap()
    @BeanProperty var sbtPassParentEnvironment: Boolean = true

    private val linkedProjectSettings: j.TreeSet[SbtProjectSettings] = new j.TreeSet[SbtProjectSettings]

    @AbstractCollection(surroundWithTag = false, elementTypes = Array(classOf[SbtProjectSettings]))
    override def getLinkedExternalProjectsSettings: j.Set[SbtProjectSettings] =
      linkedProjectSettings

    override def setLinkedExternalProjectsSettings(settings: j.Set[SbtProjectSettings]): Unit = {
      if (settings != null) linkedProjectSettings.addAll(settings)
    }
  }
}
