package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.build.ui.FilePosition
import consulo.build.ui.event.EventResult
import consulo.build.ui.progress.{BuildProgress, BuildProgressDescriptor}
import consulo.compiler.{CompileContext, CompileContextEx, CompileDriver, CompilerRunner}
import consulo.dataContext.DataContext
import consulo.document.FileDocumentManager
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.ExternalSystemApiUtil as ES
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.module.Module
import consulo.project.Project
import consulo.virtualFileSystem.util.VirtualFileUtil
import jakarta.inject.Inject
import org.jetbrains.bsp.data.BspMetadata
import org.jetbrains.bsp.project.BspTask.BspTarget
import org.jetbrains.bsp.{BSP, BspBundle, BspUtil, Icons}
import org.jetbrains.plugins.scala.build.BuildMessages.EventId
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter, CompositeReporter, IndicatorReporter}
import org.jetbrains.plugins.scala.bsp.build.CompilerEventReporter
import org.jetbrains.plugins.scala.extensions
import org.jetbrains.plugins.scala.shared.util.CompilationId

import java.nio.file.{Path, Paths}
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/**
 * Builds BSP modules through the BSP server instead of the built-in Consulo compiler.
 *
 * upstream: BspProjectTaskRunner (IntelliJ ProjectTaskRunner) - the targets of the affected BSP modules are compiled by a
 * [[BspTask]] which runs in the compile thread and reports to the Consulo build progress and compile messages;
 * the "notify completed BSP build" CompilerTopics.COMPILATION_STATUS hack is not needed (the compile driver publishes it).
 */
@ExtensionImpl
final class BspCompilerRunner @Inject()(project: Project) extends CompilerRunner {

  import BspCompilerRunner.*

  override def getName: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.task.build"))

  override def checkAvailable(dataContext: DataContext): CompilerRunner.Result =
    if (!project.isDisposed && BspUtil.isBspProject(project)) YesResult
    else CompilerRunner.NO

  override def build(
    compileDriver: CompileDriver,
    context: CompileContextEx,
    buildProgress: BuildProgress[BuildProgressDescriptor],
    isRebuild: Boolean,
    forceCompile: Boolean,
    onlyCheckStatus: Boolean
  ): Boolean = {
    if (onlyCheckStatus)
      return false

    val modules = context.getCompileScope.getAffectedModules.toSeq.filter(BspUtil.isBspModule)
    val targets = modules.flatMap(targetsOf(project, _))
    val targetsToClean = if (isRebuild) targets else Seq.empty

    // Asked to build modules, but no target found for any: running it would complete as a success that built nothing.
    if (targets.isEmpty) {
      Log.warn(s"No BSP target found for the modules ${modules.map(_.getName)}; nothing was built")
      return false
    }

    // TODO save only documents in affected targets?
    extensions.invokeAndWait {
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    val indicator = context.getProgressIndicator
    val reporter = new CompositeReporter(
      new CompileContextReporter(context, buildProgress),
      new CompilerEventReporter(
        project,
        CompilationId(timestamp = System.nanoTime(), documentVersions = immutable.HashMap.empty),
        buildReason = Some(if (targetsToClean.nonEmpty) "Rebuild" else "Compile"),
        module = Option.when(targets.nonEmpty)(targets.map(_.target.toString).mkString(", "))
      ),
      new IndicatorReporter(indicator)
    )

    val bspTask = new BspTask(project, targets, targetsToClean, Some(CustomTaskArguments(BspBundle.message("bsp.task.build"), reporter)))
    Try(bspTask.run(indicator)).failed.foreach(bspTask.onThrowable)

    Try(Await.result(bspTask.resultFuture, Duration.Inf)) match {
      case Success(messages) =>
        if (messages.status == BuildMessages.Error && messages.errors.isEmpty)
          context.newError(LocalizeValue.of(BspBundle.message("bsp.task.build.failed.unknown.reason"))).add()
      case Failure(error) =>
        context.newError(LocalizeValue.of(Option(error.getMessage).getOrElse(BspBundle.message("bsp.task.build.failed.unknown.reason")))).add()
    }
    true
  }
}

private object BspCompilerRunner {
  private val Log = Logger.getInstance(classOf[BspCompilerRunner])

  private val YesResult = new CompilerRunner.YesResult(Icons.BSP)

  // TODO all these Options fail silently. collect errors and report something
  def targetsOf(project: Project, module: Module): Seq[BspTarget] = {
    val moduleId = ES.getExternalProjectId(module)
    val targetIds = for {
      projectPath <- Option(ES.getExternalProjectPath(module))
      projectData <- Option(BspExternalSystemUtil.findProjectNode(project, projectPath))
      moduleDataNode <- Option(BspExternalSystemUtil.findChild(
        projectData, ProjectKeys.MODULE,
        (node: DataNode[ModuleData]) => node.getData.getId == moduleId))
      metadata <- Option(ES.find(moduleDataNode, BspMetadata.Key))
    } yield {
      val data = metadata.getData
      val workspaceUri = Paths.get(projectPath).toUri
      data.targetIds.asScala.map(id => BspTarget(workspaceUri, id.uri)).toList
    }
    targetIds.getOrElse(List.empty)
  }

  /** Reports the BSP build output to the build progress and the diagnostics to the compile messages. */
  private final class CompileContextReporter(context: CompileContext, buildProgress: BuildProgress[BuildProgressDescriptor])
    extends BuildReporter {

    override def start(): Unit = ()
    override def finish(messages: BuildMessages): Unit = ()
    override def finishWithFailure(err: Throwable): Unit =
      context.newError(LocalizeValue.of(Option(err.getMessage).getOrElse(err.getClass.getName))).add()
    override def finishCanceled(): Unit = ()

    override def warning(message: String, position: Option[FilePosition]): Unit =
      addMessage(context.newWarning, message, position)
    override def error(message: String, position: Option[FilePosition]): Unit =
      addMessage(context.newError, message, position)
    override def info(message: String, position: Option[FilePosition]): Unit =
      addMessage(context.newInfo, message, position)
    override def clear(file: Path): Unit = ()

    override def log(message: String): Unit =
      buildProgress.output(message + System.lineSeparator(), true)
    override def logErr(message: String): Unit =
      buildProgress.output(message + System.lineSeparator(), false)

    override def startTask(eventId: EventId, parent: Option[EventId], message: String, time: Long): Unit = ()
    override def progressTask(eventId: EventId, total: Long, progress: Long, unit: String, message: String, time: Long): Unit = ()
    override def finishTask(eventId: EventId, message: String, result: EventResult, time: Long): Unit = ()

    private def addMessage(newMessage: LocalizeValue => CompileContext.MessageBuilder, message: String, position: Option[FilePosition]): Unit = {
      val builder = newMessage(LocalizeValue.of(BuildMessages.stripAnsiCodes(message)))
      // FilePosition is 0-based, compile messages are 1-based
      position.foreach { pos =>
        builder.url(VirtualFileUtil.pathToUrl(pos.getPath.toString)).position(pos.getStartLine + 1, pos.getStartColumn + 1)
      }
      builder.add()
    }
  }
}
