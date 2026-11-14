package org.jetbrains.sbt.runner

import consulo.application.ApplicationManager.getApplication
import consulo.disposer.Disposable
import consulo.execution.configuration.{ConfigurationFactory, EnvironmentAware, LegacyRunConfigurationConverter, ModuleBasedConfiguration, RunConfigurationModule, RunConfigurationOptions, RunProfileState, WorkingDirectoryAware}
import consulo.execution.configuration.fragment.{EnvironmentFragmentState, RunConfigurationFragment, RunConfigurationFragmentEditor, RunConfigurationFragmentLayout, TagRunConfigurationFragment, WorkingDirectoryFragmentState}
import consulo.execution.executor.Executor
import consulo.execution.runner.ExecutionEnvironment
import consulo.localize.LocalizeValue
import consulo.module.Module
import consulo.project.Project
import consulo.process.cmd.ParametersListUtil
import consulo.ui.TextBox
import consulo.ui.annotation.RequiredUIAccess
import consulo.util.lang.StringUtil
import consulo.util.xml.serializer.annotation.Attribute
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.util.JdomExternalizerMigrationHelper
import org.jetbrains.sbt.SbtBundle

import java.util
import scala.jdk.CollectionConverters.*

/**
 * Run configuration of sbt tasks.
 *
 * upstream: the settings are persisted by readExternal/writeExternal and edited by SbtRunConfigurationForm;
 * in Consulo they are stored in [[SbtRunConfigurationOptions]] and edited by run configuration fragments
 * (sbt commands, "use sbt shell", VM parameters + the platform working directory and environment fragments).
 * The legacy `tasks` field is no longer written back for older IDE versions.
 */
class SbtRunConfiguration(
  val project: Project,
  val configurationFactory: ConfigurationFactory,
  val name: String
)  extends ModuleBasedConfiguration[RunConfigurationModule](
  name,
  new RunConfigurationModule(project),
  configurationFactory
) with WorkingDirectoryAware with EnvironmentAware {

  override protected def getOptionsClass: Class[? <: RunConfigurationOptions] = classOf[SbtRunConfigurationOptions]

  override def getOptions: SbtRunConfigurationOptions = super.getOptions.asInstanceOf[SbtRunConfigurationOptions]

  /**
   * Sbt commands to execute. The `commands` field in the Run Configuration UI is bound to this.
   * The value of this field is used directly when executing commands in both shell and non-shell modes.
   * No transformations are made at runtime.
   *
   * @see [[org.jetbrains.sbt.runner.SbtRunConfiguration.migrateTasksToCommands]]
   */
  def commands: String = StringUtil.notNullize(getOptions.getCommands)

  def commands_=(value: String): Unit = getOptions.setCommands(value)

  /**
   * Extra java options.
   */
  def vmparams: String = StringUtil.notNullize(getOptions.getVmparams)

  def vmparams_=(value: String): Unit = getOptions.setVmparams(value)

  def useSbtShell: Boolean = getOptions.isUseSbtShell

  def useSbtShell_=(value: Boolean): Unit = getOptions.setUseSbtShell(value)

  /**
   * Environment variables.
   */
  def environmentVariables: java.util.Map[String, String] = getEnvs

  def workingDir: String = {
    val dir = getWorkingDirectory
    if (StringUtil.isEmpty(dir)) defaultWorkingDirectory else dir
  }

  private def defaultWorkingDirectory =
    if (getApplication == null || getApplication.isUnitTestMode) ""
    else Option(project.baseDir).fold("")(_.getPath)

  override def getValidModules: util.Collection[Module] = new java.util.ArrayList

  // upstream: isBuildBeforeLaunchAddedByDefault = false
  override def isCompileBeforeLaunchAddedByDefault: Boolean = false

  override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState =
    new SbtCommandLineState(commands, this, env)

  override def createFragments(): util.List[RunConfigurationFragment[?]] =
    util.List.of(new SbtRunConfigurationCommandsFragment, new SbtRunConfigurationUseSbtShellFragment, new SbtRunConfigurationVmParametersFragment)

  override protected def convertLegacyState(converter: LegacyRunConfigurationConverter): Unit = {
    super.convertLegacyState(converter)
    val element = converter.getElement

    def option(name: String): Option[String] =
      element.getChildren("option").asScala
        .find(o => name == o.getAttributeValue("name"))
        .flatMap(o => Option(o.getAttributeValue("value")))

    var tasks = option("tasks").getOrElse("")
    var commands = option("commands").getOrElse("")
    var vmparams = option("vmparams")
    var workingDir = option("workingDir")
    var useSbtShell = option("useSbtShell").map(_.toBoolean)

    JdomExternalizerMigrationHelper(element) { helper =>
      helper.migrateString("tasks")(tasks = _)
      helper.migrateString("vmparams")(v => vmparams = Some(v))
      helper.migrateString("workingDir")(v => workingDir = Some(v))
      helper.migrateBool("useSbtShell")(v => useSbtShell = Some(v))
    }

    // If `commands` has not been persisted yet but the legacy `tasks` field is present, the migration is required.
    if (commands.isEmpty && tasks.nonEmpty) {
      commands = SbtRunConfiguration.migrateTasksToCommands(tasks)
    }

    val options = new SbtRunConfigurationOptions
    options.setCommands(commands)
    vmparams.foreach(options.setVmparams)
    useSbtShell.foreach(options.setUseSbtShell)
    converter.setOptions(options)

    converter.addFragment(new WorkingDirectoryFragmentState(workingDir.orNull))
    converter.addFragment(new EnvironmentFragmentState(converter.readEnvs(), true))
  }
}

class SbtRunConfigurationOptions extends RunConfigurationOptions {
  private var myCommands: String = ""
  private var myVmparams: String = "-Xms512M -Xmx1024M -Xss1M"
  private var myUseSbtShell: Boolean = true

  @Attribute("commands")
  def getCommands: String = myCommands

  def setCommands(commands: String): Unit = myCommands = commands

  @Attribute("vmparams")
  def getVmparams: String = myVmparams

  def setVmparams(vmparams: String): Unit = myVmparams = vmparams

  @Attribute("useSbtShell")
  def isUseSbtShell: Boolean = myUseSbtShell

  def setUseSbtShell(useSbtShell: Boolean): Unit = myUseSbtShell = useSbtShell
}

private final class SbtRunConfigurationCommandsFragment extends RunConfigurationFragment[SbtRunConfiguration] {
  override def getId: String = "sbt.commands"

  override def getName: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.runner.form.tasks"))

  override def isRemovable: Boolean = false

  @RequiredUIAccess
  override def createEditor(project: Project, parent: Disposable): RunConfigurationFragmentEditor[SbtRunConfiguration] = {
    val commandsBox = TextBox.create()
    commandsBox.setPlaceholder(LocalizeValue.of(SbtBundle.message("sbt.runner.form.tasks.comment").replaceAll("<[^>]+>", "")))

    new RunConfigurationFragmentEditor[SbtRunConfiguration] {
      @RequiredUIAccess
      override def addRows(layout: RunConfigurationFragmentLayout): Unit =
        layout.addLabeled(LocalizeValue.of(SbtBundle.message("sbt.runner.form.tasks")), commandsBox)

      @RequiredUIAccess
      override def reset(target: SbtRunConfiguration): Unit =
        commandsBox.setValue(target.commands)

      @RequiredUIAccess
      override def apply(target: SbtRunConfiguration): Unit =
        target.commands = StringUtil.notNullize(commandsBox.getValue)
    }
  }
}

private final class SbtRunConfigurationUseSbtShellFragment extends TagRunConfigurationFragment[SbtRunConfiguration] {
  override def getId: String = "sbt.useSbtShell"

  override def getName: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.runner.form.use.sbt.shell"))

  override def isSet(target: SbtRunConfiguration): Boolean = target.useSbtShell

  override def set(target: SbtRunConfiguration, value: Boolean): Unit = target.useSbtShell = value
}

// upstream: VM parameters, working directory and environment are disabled in the form when "Use sbt shell" is selected
private final class SbtRunConfigurationVmParametersFragment extends RunConfigurationFragment[SbtRunConfiguration] {
  override def getId: String = "sbt.vmparams"

  override def getName: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.runner.form.vmParametersEditorTitle"))

  @RequiredUIAccess
  override def createEditor(project: Project, parent: Disposable): RunConfigurationFragmentEditor[SbtRunConfiguration] = {
    val vmParametersBox = TextBox.create()

    new RunConfigurationFragmentEditor[SbtRunConfiguration] {
      @RequiredUIAccess
      override def addRows(layout: RunConfigurationFragmentLayout): Unit =
        layout.addLabeled(LocalizeValue.of(SbtBundle.message("sbt.runner.form.vmParameters")), vmParametersBox)

      @RequiredUIAccess
      override def reset(target: SbtRunConfiguration): Unit =
        vmParametersBox.setValue(target.vmparams)

      @RequiredUIAccess
      override def apply(target: SbtRunConfiguration): Unit =
        target.vmparams = StringUtil.notNullize(vmParametersBox.getValue)
    }
  }
}

private object SbtRunConfiguration {
  import scala.jdk.CollectionConverters.*

  /**
   * Converts the old `tasks` format to the new `commands` format (semicolon-separated, no quotes).
   *
   * This is needed to preserve compatibility when a configuration created in an older plugin version is opened in the current one.
   * In older plugin versions, the space-separated `tasks` string was joined with semicolons and quotes were removed at runtime.
   * For example, the configuration `task1 task2` was transformed at runtime to `;task1; task2` and it worked correctly. Without
   * migrating from the old `tasks` format to the new `commands` format, `task1 task2` would not work in the current plugin version
   * because we no longer join the string with semicolons at runtime — we require semicolons explicitly. That is why the migration is needed.
   *
   * How migration works:
   *
   * An input whose trimmed form starts with `;` is returned as-is. The old runtime skipped
   * all processing for such inputs (https://github.com/JetBrains/intellij-scala/commit/45f24a440b9a10bd1a8c1ff49616647e3513f93a),
   * and the current runtime passes `commands` to sbt unchanged, so no migration is needed.
   *
   * Otherwise, the old `tasks` string is split on unquoted whitespace with `ParametersListUtil.parse` (the same as it was done in the past),
   * which also drops real quotes and unescapes inner `\"`. The resulting tokens are then re-joined, and the separator
   * is chosen based on whether the original `tasks` string contains an unquoted `;`. If it does, the user already used
   * the proper `;`-separated format, so we join the tokens with a space; otherwise we join them with `;`, as in the past.
   *
   * Examples (tasks -> commands):
   *  - `;echo "foo"` → `;echo "foo"` (leading `;`, returned as-is)
   *  - `clean compile` → `clean; compile` (no `;`: joined with `;`)
   *  - `"clean; compile"` → `clean; compile` (outer quotes stripped)
   *  - `"echo \"fo;o\""` → `echo "fo;o"` (outer quotes stripped, inner escaped quotes unescaped)
   *  - `clean "echo \"a;b\""` → `clean; echo "a;b"` (the only `;` is quoted, so we join pieces with `;`)
   *  - `""` → `""`
   *
   * @see [[org.jetbrains.sbt.runner.SbtRunConfigurationMigrationTest#testMigrateTasksToCommands]]
   */
  def migrateTasksToCommands(tasks: String): String =
    if (tasks.isBlank || tasks.trim.startsWith(";")) tasks
    else {
      // `splitHonorQuotes` honors single quotes, so for an input whose only semicolons are
      // inside single quotes, it returns a single element and `;` is chosen as the separator.
      // This should not be harmful:
      // - `;` is the separator used by the old `tasks` processing, so the migrated string matches the previous behavior
      // - sbt does not honor single quotes, so a command like this is most likely invalid anyway
      val containsUnquotedSemicolon = StringUtil.splitHonorQuotes(tasks, ';').size() > 1
      val sep = if containsUnquotedSemicolon then " " else "; "
      ParametersListUtil.parse(tasks).asScala.mkString(sep)
    }

  /**
   * Converts the new `commands` format back to the old `tasks` format.
   *
   * This is the reverse of [[migrateTasksToCommands]] and is needed to keep the persisted `tasks` field
   * in sync when the user edits the current `commands` field. Older plugin versions preprocessed the `tasks`
   * value at runtime with `ParametersListUtil.parse`, which splits on unquoted spaces (dropping quotes) and then
   * force-joins the resulting parts with semicolons. So a value with an unquoted space (e.g. `task1; task2`) was turned
   * at runtime into `;task1; ;task2` (unless it was wrapped in quotes).
   *
   * The goal of this migration is to wrap in quotes any `commands` value that contains an unquoted space, so it
   * keeps behaving the same under the old plugin versions. A value whose spaces are already inside quotes (e.g.
   * `task1;"add 1 2"`), or that is a single word without any spaces (e.g. `task1`), is kept as-is, because the old
   * runtime's `parse` already treats it as a single command.
   *
   * Examples (commands → tasks):
   *  - `task1` → `task1` (returned as-is)
   *  - `task1; task1` → `"task1; task1"` (unquoted space, so wrapped in quotes)
   *  - `add 1 2` → `"add 1 2"` (unquoted spaces, so wrapped in quotes)
   *  - `task1;"add 1 2"` → `task1;"add 1 2"` (spaces only inside quotes, returned as-is)
   *  - `"task1; task1"` → `"task1; task1"` (already quoted, returned as-is)
   *
   * Rules:
   *  - Blank or already quoted → returned as-is.
   *  - Contains an unquoted space → wrapped in quotes via `ParametersListUtil.join`.
   *  - Otherwise → returned as-is.
   *
   * @see [[org.jetbrains.sbt.runner.SbtRunConfigurationMigrationTest#testMigrateCommandsToTasks]]
   */
  def migrateCommandsToTasks(commands: String): String =
    if (commands.isBlank || StringUtil.isQuotedString(commands)) commands
    else {
      // `keepEmptyParameters = true` keeps the input untrimmed so edge whitespace (e.g. `  task1  `) also
      // counts as an unquoted space and gets wrapped.
      val shouldJoin = ParametersListUtil.parse(commands, /*keepQuotes*/ false, /*supportSingleQuotes*/ false, /*keepEmptyParameters*/ true).size > 1
      if (shouldJoin)
        ParametersListUtil.join(commands)
      else
        commands
    }
}