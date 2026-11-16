package org.jetbrains.bsp.settings

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl, TopicAPI}
import consulo.application.ApplicationManager
import consulo.component.persist.{PersistentStateComponent, State, Storage, StoragePathMacros}
import consulo.disposer.Disposable
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings
import consulo.externalSystem.service.setting.{AbstractExternalProjectSettingsConfigurable, ExternalSystemSettingsConfigurable, ExternalSystemSettingsPlace}
import consulo.externalSystem.setting.{AbstractExternalSystemLocalSettings, AbstractExternalSystemSettings, DelegatingExternalSystemSettingsListener, ExternalProjectSettings, ExternalSystemSettingsListener}
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{LabeledLayout, VerticalLayout}
import consulo.ui.{CheckBox, Component}
import consulo.util.xml.serializer.Converter
import consulo.util.xml.serializer.annotation.{AbstractCollection, OptionTag}
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.annotations.Nullable
import org.jetbrains.bsp._
import org.jetbrains.bsp.settings.BspProjectSettings._
import org.jetbrains.bsp.settings.PreImportConfig.AutoPreImport
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.{Path, Paths}
import java.util
import scala.beans.BeanProperty

class BspProjectSettings extends ExternalProjectSettings {

  @BeanProperty
  var buildOnSave = false

  @BeanProperty
  var runPreImportTask = true

  @BeanProperty
  @OptionTag(converter = classOf[BspServerConfigConverter])
  var serverConfig: BspServerConfig = AutoConfig

  @BeanProperty
  var preImportConfig: PreImportConfig = AutoPreImport

  /** Whether the Scala plugin generated the BSP connection file during initial import */
  @BeanProperty
  var bspConfigGenerated: Boolean = false

  /**
   * Hash of the BSP connection files under the .bsp directory
   *
   * @see [[org.jetbrains.bsp.impl.protocol.BspConnectionConfig.workspaceBspConfigsHash]]
   */
  @BeanProperty
  @Nullable
  var connectionFileHash: Integer = null

  /**
   * Whether to automatically regenerate the BSP connection file before the BSP server starts.
   * This is only applied to Scala CLI or Mill projects.
   */
  @BeanProperty
  var autoRegenerateBspConfigOnServerStartup = false

  override def setExternalProjectPath(externalProjectPath: String): Unit = {
    super.setExternalProjectPath(ExternalSystemApiUtil.toCanonicalPath(externalProjectPath))
  }

  override def clone(): BspProjectSettings = {
    val result = new BspProjectSettings
    copyTo(result)
    result.buildOnSave = buildOnSave
    result.runPreImportTask = runPreImportTask
    result.serverConfig = serverConfig
    result.preImportConfig = preImportConfig
    result.bspConfigGenerated = bspConfigGenerated
    result.connectionFileHash = connectionFileHash
    result.autoRegenerateBspConfigOnServerStartup = autoRegenerateBspConfigOnServerStartup
    result
  }
}

object BspProjectSettings {

  /**
   * A specific configuration to start and connect to a BSP server.
   *
   * '''Important:''' this class is used as a type for one of the fields inside [[BspProjectSettings]] and is serialized.
   * By default, the IntelliJ mechanism for checking whether the new value of [[BspProjectSettings.serverConfig]]
   * is different from the default, and consequently whether it should be serialized or not, relies on comparing
   * the fields inside the class. Since this class has no fields, e.g., `AutoConfig` and `BloopConfig` were considered
   * equal (see [[https://youtrack.jetbrains.com/issue/IJPL-231922]]).
   *
   * To address this, a custom `equals` method is implemented inside this class. It switches the comparison
   * from fields-based to `equals`-method-based, ensuring the [[BspProjectSettings.serverConfig]] is properly serialized when required.
   *
   * This is just a workaround. If the ADT has no fields, like in
   * [[org.jetbrains.bsp.settings.PreImportConfig]], the simplest solution is to use a Java enum.
   *
   *   - When adding new subtypes ensure the `equals`/`hashCode` methods are updated and work correctly.
   *   - '''In case of any changes inside [[BspServerConfig]] and its subtypes adjust tests in [[org.jetbrains.bsp.settings.BspProjectSettingsTest]]'''
   */
  sealed abstract class BspServerConfig {
    override def equals(obj: Any): Boolean = {
      if (!obj.isInstanceOf[BspServerConfig] || this.getClass != obj.getClass) return false

      if (this.isInstanceOf[BspConfigFile]) {
        val _this = this.asInstanceOf[BspConfigFile]
        val other = obj.asInstanceOf[BspConfigFile]
        _this.path.toAbsolutePath.normalize() == other.path.toAbsolutePath.normalize()
      } else {
        true // case objects with same class are equal
      }
    }

    override def hashCode(): Int = this match {
      case AutoConfig => "AutoConfig".hashCode
      case BloopConfig => "BloopConfig".hashCode
      case BspConfigFile(path) => 31 * getClass.hashCode() + path.toAbsolutePath.normalize().hashCode()
    }
  }
  /** Choose BSP config automatically */
  case object AutoConfig extends BspServerConfig
  /** Bloop without preimport */
  case object BloopConfig extends BspServerConfig
  /** Use BSP config file to specify connection */
  case class BspConfigFile(path: Path) extends BspServerConfig

  private class BspServerConfigConverter extends Converter[BspServerConfig] {
    private val configFile = "BspConfigFile:(?<path>.*)".r
    override def fromString(value: String): BspServerConfig = {
      value match {
        case "AutoConfig" => AutoConfig
        case "BloopConfig" => BloopConfig
        case configFile(path) => BspConfigFile(Paths.get(path))
      }
    }

    override def toString(value: BspServerConfig): String =
      value match {
        case AutoConfig => "AutoConfig"
        case BloopConfig => "BloopConfig"
        case BspConfigFile(path) => s"BspConfigFile:$path"
      }
  }
}

// upstream: AbstractExternalProjectSettingsControl with a Swing panel; the settings without check boxes (pre-import config,
// server config, ...) are kept by editing the given settings object in place
class BspProjectSettingsControl(settings: BspProjectSettings, place: ExternalSystemSettingsPlace = ExternalSystemSettingsPlace.SETTINGS)
  extends AbstractExternalProjectSettingsConfigurable[BspProjectSettings](settings, place) {

  private var buildOnSaveCheckBox: CheckBox = scala.compiletime.uninitialized
  private var runPreImportTaskCheckBox: CheckBox = scala.compiletime.uninitialized
  private var autoRegenerateBspConfigCheckBox: CheckBox = scala.compiletime.uninitialized

  @RequiredUIAccess
  override protected def createExtraUIComponent(uiDisposable: Disposable): Component = {
    val initial = getSettings
    buildOnSaveCheckBox = CheckBox.create(LocalizeValue.of(BspBundle.message("bsp.protocol.build.automatically.on.file.save")), initial.buildOnSave)
    runPreImportTaskCheckBox = CheckBox.create(LocalizeValue.of(BspBundle.message("bsp.protocol.export.sbt.projects.to.bloop.before.import")), initial.runPreImportTask)
    autoRegenerateBspConfigCheckBox = CheckBox.create(LocalizeValue.of(BspBundle.message("bsp.protocol.auto.generate.config")), initial.autoRegenerateBspConfigOnServerStartup)
    autoRegenerateBspConfigCheckBox.setToolTipText(LocalizeValue.of(BspBundle.message("bsp.protocol.auto.generate.config.tooltip")))

    val layout = VerticalLayout.create()
    layout.add(buildOnSaveCheckBox)
    layout.add(LabeledLayout.create(LocalizeValue.of(BspBundle.message("bsp.protocol.sbt.project")), runPreImportTaskCheckBox))
    layout.add(LabeledLayout.create(LocalizeValue.of(BspBundle.message("bsp.protocol.scala.cli.mill.project")), autoRegenerateBspConfigCheckBox))
    layout
  }

  @RequiredUIAccess
  override protected def isExtraModified: Boolean = {
    val initial = getSettings
    buildOnSaveCheckBox != null && (
      buildOnSaveCheckBox.getValue.booleanValue != initial.buildOnSave ||
        runPreImportTaskCheckBox.getValue.booleanValue != initial.runPreImportTask ||
        autoRegenerateBspConfigCheckBox.getValue.booleanValue != initial.autoRegenerateBspConfigOnServerStartup
      )
  }

  @RequiredUIAccess
  override protected def applyExtra(): Unit = {
    if (buildOnSaveCheckBox == null) return
    val settings = getSettings
    settings.buildOnSave = buildOnSaveCheckBox.getValue
    settings.runPreImportTask = runPreImportTaskCheckBox.getValue
    settings.autoRegenerateBspConfigOnServerStartup = autoRegenerateBspConfigCheckBox.getValue
  }

  @RequiredUIAccess
  override def reset(): Unit = {
    if (buildOnSaveCheckBox == null) return
    val initial = getSettings
    buildOnSaveCheckBox.setValue(initial.buildOnSave)
    runPreImportTaskCheckBox.setValue(initial.runPreImportTask)
    autoRegenerateBspConfigCheckBox.setValue(initial.autoRegenerateBspConfigOnServerStartup)
  }
}


/** A dummy to satisfy interface constraints of ExternalSystem */
@TopicAPI(ComponentScope.PROJECT)
trait BspProjectSettingsListener extends ExternalSystemSettingsListener[BspProjectSettings] {
  // the message bus dispatches only methods declared on the topic interface, so the inherited ones are redeclared
  override def onProjectRenamed(oldName: String, newName: String): Unit = {}
  override def onProjectsLoaded(settings: util.Collection[BspProjectSettings]): Unit = {}
  override def onProjectsLinked(settings: util.Collection[BspProjectSettings]): Unit = {}
  override def onProjectsUnlinked(linkedProjectPaths: util.Set[String]): Unit = {}
  override def onBulkChangeStart(): Unit = {}
  override def onBulkChangeEnd(): Unit = {}
}

class BspProjectSettingsListenerAdapter(listener: ExternalSystemSettingsListener[BspProjectSettings])
  extends DelegatingExternalSystemSettingsListener[BspProjectSettings](listener) with BspProjectSettingsListener {
  override def onProjectRenamed(oldName: String, newName: String): Unit =
    super[DelegatingExternalSystemSettingsListener].onProjectRenamed(oldName, newName)
  override def onProjectsLoaded(settings: util.Collection[BspProjectSettings]): Unit =
    super[DelegatingExternalSystemSettingsListener].onProjectsLoaded(settings)
  override def onProjectsLinked(settings: util.Collection[BspProjectSettings]): Unit =
    super[DelegatingExternalSystemSettingsListener].onProjectsLinked(settings)
  override def onProjectsUnlinked(linkedProjectPaths: util.Set[String]): Unit =
    super[DelegatingExternalSystemSettingsListener].onProjectsUnlinked(linkedProjectPaths)
  override def onBulkChangeStart(): Unit =
    super[DelegatingExternalSystemSettingsListener].onBulkChangeStart()
  override def onBulkChangeEnd(): Unit =
    super[DelegatingExternalSystemSettingsListener].onBulkChangeEnd()
}

@State(
  name = "BspSettings",
  storages = Array(new Storage("bsp.xml"))
)
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
class BspSettings @Inject()(project: Project)
  extends AbstractExternalSystemSettings[BspSettings, BspProjectSettings, BspProjectSettingsListener](BspSettings.BspTopic, project)
    with PersistentStateComponent[BspSettings.State]
{

  def getSystemSettings: BspSystemSettings = BspSystemSettings.getInstance

  override def subscribe(listener: ExternalSystemSettingsListener[BspProjectSettings]): Unit =
    subscribe(listener, project)

  def subscribe(listener: ExternalSystemSettingsListener[BspProjectSettings], parentDisposable: Disposable): Unit =
    project.getMessageBus.connect(parentDisposable).subscribe(BspSettings.BspTopic, new BspProjectSettingsListenerAdapter(listener))

  override def copyExtraSettingsFrom(settings: BspSettings): Unit = {}

  override def checkSettings(old: BspProjectSettings, current: BspProjectSettings): Unit = {}

  override def getState: BspSettings.State = {
    val state = new BspSettings.State
    fillState(state)
    state
  }

  override def loadState(state: BspSettings.State): Unit = {
    super[AbstractExternalSystemSettings].loadState(state)
  }
}

object BspSettings {

  class State extends AbstractExternalSystemSettings.State[BspProjectSettings] {

    private val projectSettings = new util.TreeSet[BspProjectSettings]

    @AbstractCollection(surroundWithTag = false, elementTypes = Array(classOf[BspProjectSettings]))
    override def getLinkedExternalProjectsSettings: util.Set[BspProjectSettings] = projectSettings
    override def setLinkedExternalProjectsSettings(settings: util.Set[BspProjectSettings]): Unit =
      projectSettings.addAll(settings)
  }

  def getInstance(project: Project): BspSettings = project.getInstance(classOf[BspSettings])

  val BspTopic: Class[BspProjectSettingsListener] = classOf[BspProjectSettingsListener]
}


@State(
  name = "BspSystemSettings",
  storages = Array(new Storage("bsp.settings.xml"))
)
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
class BspSystemSettings extends PersistentStateComponent[BspSystemSettings.State] {

  @BeanProperty
  var myState: BspSystemSettings.State = new BspSystemSettings.State

  override def getState: BspSystemSettings.State = myState

  override def loadState(state: BspSystemSettings.State): Unit = {
    myState = state
  }
}

object BspSystemSettings {
  def getInstance: BspSystemSettings = ApplicationManager.getApplication.getInstance(classOf[BspSystemSettings])

  class State {
    @BeanProperty
    var traceBsp: Boolean = false
  }
}


@State(
  name = "BspLocalSettings",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
class BspLocalSettings @Inject()(project: Project)
  extends AbstractExternalSystemLocalSettings(BSP.ProjectSystemId, project)
    with PersistentStateComponent[BspLocalSettingsState] {

  override def getState: BspLocalSettingsState = {
    val state = new BspLocalSettingsState
    fillState(state)
    state
  }

  override def loadState(state: BspLocalSettingsState): Unit =
    super[AbstractExternalSystemLocalSettings].loadState(state)
}

object BspLocalSettings {
  def getInstance(project: Project): BspLocalSettings = project.getInstance(classOf[BspLocalSettings])
}

class BspLocalSettingsState extends AbstractExternalSystemLocalSettings.State

class BspExecutionSettings(val basePath: Path,
                           val traceBsp: Boolean,
                           val runPreImportTask: Boolean,
                           val preImportTask: PreImportConfig,
                           val config: BspServerConfig
                          ) extends ExternalSystemExecutionSettings

object BspExecutionSettings {

  def executionSettingsFor(project: Project, basePath: Path): BspExecutionSettings = {
    if (project == null) executionSettingsFor(basePath)
    val bspSettings = BspSettings.getInstance(project)
    val bspTraceLog = BspSystemSettings.getInstance.getState.traceBsp
    val linkedSettings = Option(bspSettings.getLinkedProjectSettings(basePath.toCanonicalPath.toString))
    val runPreImportTask = linkedSettings.forall(_.runPreImportTask)
    val preImportConfig = linkedSettings.map(_.preImportConfig).getOrElse(AutoPreImport)
    val serverConfig = linkedSettings.map(_.serverConfig).getOrElse(AutoConfig)

    new BspExecutionSettings(basePath, bspTraceLog, runPreImportTask, preImportConfig, serverConfig)
  }

  def executionSettingsFor(basePath: Path): BspExecutionSettings = {
    val systemSettings = BspSystemSettings.getInstance
    val defaultProjectSettings = new BspProjectSettings
    new BspExecutionSettings(
      basePath, systemSettings.getState.traceBsp, defaultProjectSettings.runPreImportTask, AutoPreImport, AutoConfig)
  }
}

// upstream: Swing BspSystemSettingsForm
class BspSystemSettingsControl(settings: BspSettings) extends ExternalSystemSettingsConfigurable[BspSettings](settings) {

  private val systemSettings = settings.getSystemSettings
  private var bspTraceCheckbox: CheckBox = scala.compiletime.uninitialized

  @RequiredUIAccess
  override def createUIComponent(uiDisposable: Disposable): Component = {
    bspTraceCheckbox = CheckBox.create(LocalizeValue.of(BspBundle.message("bcp.trace.log.enable")), systemSettings.getState.traceBsp)
    LabeledLayout.create(LocalizeValue.of(BspBundle.message("bsp.trace.log")), bspTraceCheckbox)
  }

  @RequiredUIAccess
  override def reset(): Unit =
    if (bspTraceCheckbox != null) bspTraceCheckbox.setValue(systemSettings.getState.traceBsp)

  @RequiredUIAccess
  override def isModified: Boolean =
    bspTraceCheckbox != null && bspTraceCheckbox.getValue.booleanValue != systemSettings.getState.traceBsp

  @RequiredUIAccess
  override def apply(): Unit =
    if (bspTraceCheckbox != null) systemSettings.getState.traceBsp = bspTraceCheckbox.getValue
}
