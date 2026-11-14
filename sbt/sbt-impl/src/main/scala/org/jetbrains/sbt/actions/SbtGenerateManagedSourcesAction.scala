package org.jetbrains.sbt.actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.application.Application
import consulo.build.ui.event.{BuildEventFactory, EventResult}
import consulo.build.ui.{DefaultBuildDescriptor, SyncViewManager}
import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskType}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.document.FileDocumentManager
import consulo.application.progress.{ProgressIndicator, Task}
import consulo.virtualFileSystem.util.VirtualFileUtil
import consulo.virtualFileSystem.VirtualFileManager
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.build.BuildMessages
import org.jetbrains.sbt.api.icons.Icons
import org.jetbrains.sbt.process.SbtRunner
import org.jetbrains.sbt.project.{SbtExternalSystemManager, SbtProjectSystem}
import org.jetbrains.sbt.{SbtBundle, SbtUtil, normalizedLocalPath, platform}
import org.jetbrains.sbt.api.SbtVersionCapabilities

import java.nio.file.{Files, Path}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

@ActionImpl(id = "Scala.Sbt.GenerateManagedSources", parents = Array(new ActionParentRef(value = new ActionRef(id = "ExternalSystemView.ActionsToolbar"), anchor = ActionRefAnchor.AFTER, relatedToAction = new ActionRef(id = "ExternalSystem.RefreshAllProjects"))))
final class SbtGenerateManagedSourcesAction extends AnAction(
  LocalizeValue.of(SbtBundle.message("sbt.generate.managed.sources.action.title")),
  LocalizeValue.of(SbtBundle.message("sbt.generate.managed.sources.action.description")),
  Icons.SBT_GENERATE_MANAGED_SOURCES
) with AnActionWithSyncUpdate {

  @RequiredUIAccess
  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project eq null) return

    val task = new Task.Backgroundable(project, LocalizeValue.of(SbtBundle.message("sbt.generate.managed.sources.task.progress.title")), true) {
      override def run(indicator: ProgressIndicator): Unit = {
        val viewManager = project.getInstance(classOf[SyncViewManager])
        val taskId = ExternalSystemTaskId.create(SbtProjectSystem.Id, ExternalSystemTaskType.EXECUTE_TASK, project)
        val reporter = new GenerateManagedSourcesReporter(viewManager, taskId)
        val factory = Application.get().getInstance(classOf[BuildEventFactory])

        def output(text: String): Unit =
          viewManager.onEvent(taskId, factory.createOutputBuildEvent(taskId, text, true))

        def finish(@Nullable message: String, result: EventResult): Unit =
          viewManager.onEvent(taskId, factory.createFinishBuildEvent(taskId, null, System.currentTimeMillis(), LocalizeValue.of(message), result))

        val settings = SbtExternalSystemManager.executionSettingsFor(project)
        val projectBasePath = Path.of(settings.realProjectPath)

        val descriptor = new DefaultBuildDescriptor(taskId, LocalizeValue.of(SbtBundle.message("sbt.generate.managed.sources.action.title")), projectBasePath.toString, System.currentTimeMillis())
        descriptor.setActivateToolWindowWhenAdded(false)
        descriptor.setActivateToolWindowWhenFailed(true)

        {
          val event = factory.createStartBuildEvent(descriptor, LocalizeValue.of(SbtBundle.message("sbt.generate.managed.sources.action.title")))
          viewManager.onEvent(taskId, event)
        }

        def reportFailure(@Nullable throwable: Throwable): Unit = {
          {
            val sbtOutput = reporter.outputLines.mkString(start = "", sep = System.lineSeparator(), end = System.lineSeparator())
            output(sbtOutput)
          }
          {
            val failureWord = SbtBundle.message("sbt.generate.managed.sources.task.result.failure")
            val failureMessage = SbtBundle.message("sbt.generate.managed.sources.task.result.failure.message")
            val failureResult = factory.newFailure().message(LocalizeValue.of(failureMessage)).optionalError(throwable).createResult()
            finish(failureWord, failureResult)
          }
        }

        try {
          val launcher = SbtUtil.getLauncherJar(settings)

          val sbtVersion = SbtUtil.detectSbtVersion(projectBasePath, launcher)
          val addPluginCommandSupported = SbtVersionCapabilities.isAddPluginCommandSupported(sbtVersion)

          if (!addPluginCommandSupported) {
            val notSupportedWord = SbtBundle.message("sbt.generate.managed.sources.action.not.supported")
            val notSupportedMessage = SbtBundle.message("sbt.generate.managed.sources.action.not.supported.message", sbtVersion.minor)
            val failureResult = factory.newFailure().message(LocalizeValue.of(notSupportedMessage)).createResult()
            finish(notSupportedWord, failureResult)
            return
          }

          val descriptor = projectBasePath.platform
          val sbtFileContent = SbtUtil.sbtStructurePluginDeclaration(
            sbtVersion,
            SbtUtil.getRepoDir(descriptor)
          ).mkString(System.lineSeparator())
          val tmpPluginsSbtFile = SbtUtil.createTemporarySbtFile(sbtFileContent, descriptor, Option(project))
          val setupOptions = Seq(s"-addPluginSbtFile=${tmpPluginsSbtFile.normalizedLocalPath}")

          val generateCommand = "show " + SbtUtil.sbtStructureGlobalCommand("ideaGenerateAllManagedSources", sbtVersion)
          val sbtResult = SbtRunner().runSbt(
            indicator,
            projectBasePath,
            settings.vmExecutable.toPath,
            settings.vmOptions,
            settings.userSetEnvironment,
            launcher,
            settings.sbtOptions,
            setupOptions,
            generateCommand,
            SbtBundle.message("sbt.generate.managed.sources.task.progress.title"),
            settings.passParentEnvironment,
            project = Some(project),
          )(using reporter)

          sbtResult match {
            case Success(buildMessages) if buildMessages.status == BuildMessages.Error => reportFailure(null)

            case Success(buildMessages) if buildMessages.status == BuildMessages.Canceled =>
              {
                val canceledMessage = SbtBundle.message("sbt.generate.managed.sources.task.result.canceled.message")
                output(canceledMessage)
              }
              {
                val canceledWord = SbtBundle.message("sbt.generate.managed.sources.task.result.canceled")
                finish(canceledWord, factory.createSkippedResult())
              }

            case Success(_) =>
              val lines = reporter.outputLines
              val containsErrors = lines.exists(_.startsWith("[error]"))

              if (containsErrors) {
                reportFailure(null)
              } else {
                try {
                  def realFile(path: Path): Boolean = Files.exists(path) && Files.isRegularFile(path)
                  val generatedSources = lines.collect { case s"[info] * $path" => path }
                    .flatMap(path => Try(Path.of(path).toRealPath()).filter(realFile).toOption)
                  val fileManager = VirtualFileManager.getInstance()
                  val virtualFiles = generatedSources.flatMap(p => Option(fileManager.refreshAndFindFileByNioPath(p)))
                  VirtualFileUtil.markDirtyAndRefresh(false, false, true, virtualFiles*)

                  {
                    val text = lines.mkString(start = "", sep = System.lineSeparator(), end = System.lineSeparator())
                    output(text)
                  }
                  {
                    finish(SbtBundle.message("sbt.generate.managed.sources.task.result.success"), factory.createSuccessResult())
                  }
                } catch {
                  case NonFatal(t) => reportFailure(t)
                }
              }

            case Failure(exception) => reportFailure(exception)
          }
        } catch {
          case NonFatal(t) => reportFailure(t)
        }
      }
    }

    // Make sure all modified files are saved to disk before invoking sbt.
    FileDocumentManager.getInstance().saveAllDocuments()
    task.queue()
  }

  @RequiredUIAccess
  override def update(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project eq null) return
    val enabled = SbtUtil.isSbtProject(project)
    e.getPresentation.setEnabledAndVisible(enabled)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
