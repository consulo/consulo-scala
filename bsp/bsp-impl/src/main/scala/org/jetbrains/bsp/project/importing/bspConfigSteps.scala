package org.jetbrains.bsp.project.importing

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.application.ApplicationManager
import consulo.application.progress.{ProgressIndicator, Task}
import consulo.content.bundle.{Sdk, SdkTable}
import consulo.disposer.Disposable
import consulo.externalSystem.importing.ExternalModuleImportContext
import consulo.localize.LocalizeValue
import consulo.module.ui.{BundleBox, BundleBoxBuilder}
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.wizard.{WizardStep, WizardStepValidationException}
import consulo.ui.layout.{LabeledLayout, VerticalLayout}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{Component, ListBox}
import consulo.virtualFileSystem.LocalFileSystem
import org.jetbrains.bsp.project.importing.BspSetupConfigStep.BspConfigSetupTask
import org.jetbrains.bsp.project.importing.bspConfigSteps.*
import org.jetbrains.bsp.project.importing.setup.{BspConfigSetup, BspSetupProvider, NoConfigSetup, SbtConfigSetup}
import org.jetbrains.bsp.protocol.BspConnectionConfig
import org.jetbrains.bsp.settings.BspProjectSettings
import org.jetbrains.bsp.settings.BspProjectSettings.*
import org.jetbrains.bsp.settings.PreImportConfig
import org.jetbrains.bsp.settings.PreImportConfig.*
import org.jetbrains.bsp.{BspBundle, BspJdkUtil, BspUtil}
import org.jetbrains.plugins.scala.build.IndicatorReporter
import org.jetbrains.plugins.scala.project.external.SdkUtils
import org.jetbrains.sbt.SbtUtil.*
import org.jetbrains.sbt.project.SbtProjectImportProvider
import org.jetbrains.sbt.{SbtUtil, platform}
import org.jetbrains.sbt.api.SbtVersion

import java.nio.file.Path
import java.util.concurrent.CompletableFuture

object bspConfigSteps {

  sealed abstract class ConfigSetup
  case object NoSetup extends ConfigSetup
  case object SbtSetup extends ConfigSetup
  case object BloopSetup extends ConfigSetup
  case object BloopSbtSetup extends ConfigSetup
  case object MillSetup extends ConfigSetup
  case object ScalaCliSetup extends ConfigSetup

  private[importing] def configChoiceName(configs: ConfigSetup) = configs match {
    case NoSetup => BspBundle.message("bsp.config.steps.choice.no.setup")
    case SbtSetup => BspBundle.message("bsp.config.steps.choice.sbt")
    case BloopSetup => BspBundle.message("bsp.config.steps.choice.bloop")
    case BloopSbtSetup => BspBundle.message("bsp.config.steps.choice.sbt.with.bloop")
    case MillSetup => BspBundle.message("bsp.config.steps.choice.mill")
    case ScalaCliSetup => BspBundle.message("bsp.config.steps.choice.scalaCli")
  }

  private[importing] def configName(config: BspConnectionDetails) =
    s"${config.getName} ${config.getVersion}"

  def configSetupChoices(workspace: Path): List[ConfigSetup] = {
    val workspaceConfigs = workspaceSetupChoices(workspace)
    if (workspaceConfigs.nonEmpty) workspaceConfigs
    else List(NoSetup)
  }

  // upstream: configures BspProjectImportBuilder - the Consulo import wizard keeps these values in the imported project settings
  def configureBuilder(
    jdk: Sdk,
    builder: BspProjectSettings,
    workspace: Path,
    configSetup: ConfigSetup
  ): BspConfigSetup = {
    val BuilderConfigurationParameters(
      setup: BspConfigSetup,
      preImportConfig: Option[PreImportConfig],
      serverConfig: Option[BspServerConfig]
    ) = getBuilderConfigurationParameters(jdk, workspace, configSetup)

    preImportConfig.foreach(builder.preImportConfig = _)
    serverConfig.foreach(builder.serverConfig = _)

    setup
  }

  case class BuilderConfigurationParameters(
    bspConfigSetup: BspConfigSetup,
    preImportConfig: Option[PreImportConfig],
    serverConfig: Option[BspServerConfig]
  )

  /**
   * Computes [[BuilderConfigurationParameters]] for initial project setup/wizard flows.
   * If exactly one BSP config file exists, it returns early with `NoConfigSetup` pointing to that file.
   *
   * @todo the requirement for the JDK to be present seems unnecessary for all use cases.
   *       For example, Mill or Scala CLI config setups don't require the JDK.
   */
  def getBuilderConfigurationParameters(
    jdk: Sdk,
    workspace: Path,
    configSetup: ConfigSetup
  ): BuilderConfigurationParameters = {
    val workspaceBspConfigs = BspConnectionConfig.workspaceBspConfigs(workspace)

    val (setup, preImport, server) =
      if workspaceBspConfigs.size == 1 then
        (NoConfigSetup, Some(NoPreImport), Some(BspConfigFile(workspaceBspConfigs.head._1)))
      else
        computeConfigurationTuple(jdk, workspace, configSetup)

    BuilderConfigurationParameters(setup, preImport, server)
  }

  private def computeConfigurationTuple(
    jdk: Sdk,
    workspace: Path,
    configSetup: ConfigSetup
  ): (BspConfigSetup, Option[PreImportConfig], Option[BspServerConfig]) =
    configSetup match {
      case bspConfigSteps.NoSetup =>
        (NoConfigSetup, Some(AutoPreImport), Some(AutoConfig))
      case bspConfigSteps.BloopSetup =>
        (NoConfigSetup, Some(NoPreImport), Some(BloopConfig))
      case bspConfigSteps.BloopSbtSetup =>
        (NoConfigSetup, Some(BloopSbtPreImport), Some(BloopConfig))
      case bspConfigSteps.SbtSetup =>
        (SbtConfigSetup(workspace, jdk), Some(NoPreImport), None) // server config to be set in next step
      case bspConfigSteps.MillSetup =>
        val configSetup = BspSetupProvider.getBspConfigSetup(workspace, MillSetup).getOrElse(NoConfigSetup)
        (configSetup, Some(NoPreImport), Some(AutoConfig))
      case bspConfigSteps.ScalaCliSetup =>
        val configSetup = BspSetupProvider.getBspConfigSetup(workspace, ScalaCliSetup).getOrElse(NoConfigSetup)
        (configSetup, Some(NoPreImport), Some(AutoConfig))
    }

  /**
   * Computes configuration for BSP connection file regeneration.
   * Unlike [[getBuilderConfigurationParameters]], it always generates configuration,
   * ignoring any existing connection files.
   */
  def getBspConfigurationForRegeneration(
    jdk: Sdk,
    workspace: Path,
    configSetup: ConfigSetup
  ): (BspConfigSetup, Option[PreImportConfig]) =
    computeConfigurationTuple(jdk, workspace, configSetup).take(2)

  def workspaceSetupChoices(workspace: Path): List[ConfigSetup] = {
    val vfile = LocalFileSystem.getInstance().findFileByNioFile(workspace)

    val sbtChoice = if (SbtProjectImportProvider.canImport(vfile)) {
      val sbtVersion = detectSbtVersion(workspace, SbtUtil.defaultLauncherPath)
      if (sbtVersion >= SbtVersion("1.4")) {
        // sbt >= 1.4 : user choose: bloop or sbt
        if (ApplicationManager.getApplication.isUnitTestMode) List(SbtSetup)
        else List(SbtSetup, BloopSbtSetup)
      } else {
        List(BloopSbtSetup)
      }
    } else Nil

    val millChoice =
      if (BspSetupProvider.canImport(workspace, MillSetup)) List(MillSetup)
      else Nil

    val scalaCliChoice =
      if (BspSetupProvider.canImport(workspace, ScalaCliSetup)) List(ScalaCliSetup)
      else Nil

    val bloopChoice =
      if (BspUtil.bloopConfigDir(workspace).isDefined) List(BloopSetup)
      else Nil

    (sbtChoice ++ millChoice ++ scalaCliChoice ++ bloopChoice).distinct
  }
}

/**
 * Chooses the tool generating the BSP connection file (and the JDK when there is none), then generates it.
 *
 * upstream: ModuleWizardStep of the IntelliJ import wizard (BspProjectImportBuilder keeps the choice, the setup task
 * runs in onWizardFinished) - the Consulo wizard keeps the choice in the imported project settings and the setup task
 * runs when the step is validated
 */
class BspSetupConfigStep extends WizardStep[ExternalModuleImportContext] {

  private var ui: Option[BspSetupConfigStepUi] = None

  private def settings(context: ExternalModuleImportContext): BspProjectSettings =
    context.getProjectSettings.asInstanceOf[BspProjectSettings]

  private def workspace(context: ExternalModuleImportContext): Path =
    Path.of(settings(context).getExternalProjectPath)

  private def configSetupChoices(workspace: Path): List[ConfigSetup] = {
    val workspaceSetupConfigs = workspaceSetupChoices(workspace)
    if (BspConnectionConfig.workspaceBspConfigs(workspace).size == 1) List(NoSetup)
    else if (workspaceSetupConfigs.nonEmpty) workspaceSetupConfigs
    else List(NoSetup)
  }

  private def existingJdk(context: ExternalModuleImportContext): Option[Sdk] =
    BspJdkUtil.findOrCreateBestJdkForProject(Option(context.getProject), workspace(context).platform)

  @RequiredUIAccess
  override def getComponent(context: ExternalModuleImportContext, uiDisposable: Disposable): Component = {
    val choices = configSetupChoices(workspace(context))
    val stepUi = new BspSetupConfigStepUi(
      BspBundle.message("bsp.config.steps.setup.config.choose.tool"),
      choices,
      existingJdk(context).isEmpty,
      uiDisposable
    )
    stepUi.updateChooseBspSetupComponent(choices)
    ui = Some(stepUi)
    stepUi.mainComponent
  }

  // upstream: isStepVisible also hides the step when there is only one choice and a JDK, the setup task still runs in
  // onWizardFinished - here the step must be shown to run it
  override def isVisible(context: ExternalModuleImportContext): Boolean = {
    val path = settings(context).getExternalProjectPath
    path != null &&
      settings(context).preImportConfig == AutoPreImport &&
      BspConnectionConfig.workspaceBspConfigs(Path.of(path)).isEmpty &&
      configSetupChoices(Path.of(path)) != List(NoSetup)
  }

  @RequiredUIAccess
  override def validateStep(context: ExternalModuleImportContext): CompletableFuture[?] = {
    val stepUi = ui.getOrElse(return CompletableFuture.completedFuture(null))
    if (!stepUi.isJdkSelectedIfRequired)
      return CompletableFuture.failedFuture(new WizardStepValidationException(LocalizeValue.of(BspBundle.message("jdkComboBox.validation.tooltip"))))

    val projectSettings = settings(context)
    val workspacePath = workspace(context)
    val selectedJdk = stepUi.getSelectedJdkIfRequired
    selectedJdk.foreach(SdkUtils.addJdkIfNotExists)
    val runSetupTask = existingJdk(context).orElse(selectedJdk) match {
      case Some(jdk) => configureBuilder(jdk, projectSettings, workspacePath, stepUi.selectedConfigSetup)
      case _ => NoConfigSetup
    }

    val willGenerateConfig = runSetupTask != NoConfigSetup
    projectSettings.bspConfigGenerated = willGenerateConfig

    if (!willGenerateConfig) CompletableFuture.completedFuture(null)
    else {
      val result = new CompletableFuture[AnyRef]()
      new Task.Backgroundable(context.getProject, BspBundle.message("bsp.config.steps.setup.config.task.title"), true) {
        override def run(indicator: ProgressIndicator): Unit = {
          val reporter = new IndicatorReporter(indicator)
          runSetupTask.run(indicator)(using reporter)
        }

        override def onSuccess(): Unit = result.complete(null)

        override def onCancel(): Unit = result.complete(null)

        override def onThrowable(error: Throwable): Unit =
          result.completeExceptionally(new WizardStepValidationException(LocalizeValue.of(String.valueOf(error.getMessage))))
      }.queue()
      result
    }
  }
}

object BspSetupConfigStep {

  // When `BspConfigSetupTask` is changed to run in a non-blocking way, review
  // `BspOpenProjectProvider.shouldGenerateBspConfig`, because the logic implemented there
  // relies on the config setup running synchronously during project import via "New Project from Existing Sources".
  class BspConfigSetupTask(setup: BspConfigSetup)
    extends Task.Modal(null, BspBundle.message("bsp.config.steps.setup.config.task.title"), true) {

    override def run(indicator: ProgressIndicator): Unit = {
      val reporter = new IndicatorReporter(indicator)
      setup.run(indicator)(using reporter)
    }
  }
}

// upstream: Swing panel (GridLayoutManager, JBList, JdkComboBox)
final class BspSetupConfigStepUi(
  title: String,
  configSetups: Seq[ConfigSetup],
  showJdkComboBox: Boolean,
  uiDisposable: Disposable
) {

  private val choiceNames: Seq[String] = getConfigSetupChoicesStrings(configSetups)

  val chooseBspSetupList: ListBox[String] = ListBox.create(scala.jdk.CollectionConverters.SeqHasAsJava(choiceNames).asJava)

  val jdkComboBox: Option[BundleBox] =
    Option.when(showJdkComboBox) {
      BundleBoxBuilder.create(uiDisposable)
        .withSdkTypeFilterByClass(classOf[JavaSdkType])
        .build()
    }

  val mainComponent: Component = {
    chooseBspSetupList.setToolTipText(LocalizeValue.of(BspBundle.message("bsp.config.steps.setup.config.choose.tool.tooltip")))
    val layout = VerticalLayout.create()
    layout.add(LabeledLayout.create(LocalizeValue.of(title), chooseBspSetupList))
    jdkComboBox.foreach { box =>
      val jdkLayout = LabeledBuilder.sided(LocalizeValue.localizeTODO("JDK"), box)
      layout.add(LabeledLayout.create(LocalizeValue.of(BspBundle.message("bsp.config.steps.setup.config.choose.jdk")), jdkLayout))
    }
    layout
  }

  private def selectedIndex: Int =
    Option(chooseBspSetupList.getValue).map(choiceNames.indexOf).getOrElse(-1)

  def selectedConfigSetup: ConfigSetup =
    configSetups(math.max(0, selectedIndex))

  @RequiredUIAccess
  def updateChooseBspSetupComponent(configSetupChoices: Seq[ConfigSetup]): Unit =
    if (choiceNames.nonEmpty) chooseBspSetupList.setValueByIndex(0)

  private def getConfigSetupChoicesStrings(configSetupChoices: Seq[ConfigSetup]): Seq[String] = {
    val recommendedSuffix = BspBundle.message("bsp.config.steps.choose.config.recommended.suffix")
    val configChoiceName = configSetupChoices.map(bspConfigSteps.configChoiceName)
    configChoiceName match {
      case Seq(head, tail*) =>
        s"$head ($recommendedSuffix)" +: tail
      case _ =>
        Nil
    }
  }

  def getSelectedJdkIfRequired: Option[Sdk] =
    jdkComboBox.flatMap(box => Option(box.getSelectedBundleName)).flatMap(name => Option(SdkTable.getInstance().findSdk(name)))

  def isJdkSelectedIfRequired: Boolean =
    jdkComboBox.forall(_ => getSelectedJdkIfRequired.isDefined)
}

/**
 * Chooses the BSP connection file when the workspace has more than one.
 *
 * upstream: ModuleWizardStep of the IntelliJ import wizard
 */
class BspChooseConfigStep extends WizardStep[ExternalModuleImportContext] {

  private var chooseBspConfig: Option[ListBox[String]] = None

  private def settings(context: ExternalModuleImportContext): BspProjectSettings =
    context.getProjectSettings.asInstanceOf[BspProjectSettings]

  private def bspConfigs(context: ExternalModuleImportContext) =
    BspConnectionConfig.allBspConfigs(Path.of(settings(context).getExternalProjectPath))

  @RequiredUIAccess
  override def getComponent(context: ExternalModuleImportContext, uiDisposable: Disposable): Component = {
    import scala.jdk.CollectionConverters.*
    val names = bspConfigs(context).map { case (_, details) => configName(details) }
    val list = ListBox.create(names.asJava)
    if (names.nonEmpty) list.setValueByIndex(0)
    list.setToolTipText(LocalizeValue.of(BspBundle.message("bsp.config.steps.choose.config.title.tooltip")))
    chooseBspConfig = Some(list)
    LabeledLayout.create(LocalizeValue.of(BspBundle.message("bsp.config.steps.choose.config.title")), list)
  }

  override def isVisible(context: ExternalModuleImportContext): Boolean =
    settings(context).getExternalProjectPath != null &&
      settings(context).serverConfig == AutoConfig && bspConfigs(context).size > 1

  @RequiredUIAccess
  override def validateStep(context: ExternalModuleImportContext): CompletableFuture[?] = {
    val configs = bspConfigs(context)
    val configIndex =
      if (configs.size == 1) 0
      else chooseBspConfig.flatMap(list => Option(list.getValue)).map(name => configs.indexWhere { case (_, details) => configName(details) == name }).getOrElse(-1)

    if (configIndex >= 0) {
      val (file, _) = configs(configIndex)
      settings(context).serverConfig = BspConfigFile(file)
    }
    CompletableFuture.completedFuture(null)
  }
}
