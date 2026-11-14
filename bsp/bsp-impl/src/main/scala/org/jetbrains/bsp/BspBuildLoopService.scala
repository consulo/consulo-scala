package org.jetbrains.bsp

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.{Inject, Singleton}
import consulo.application.ApplicationManager
import consulo.compiler.{CompileContext, CompileStatusNotification, CompilerManager}
import consulo.component.messagebus.MessageBusConnection
import consulo.ui.ModalityState
import consulo.module.Module
import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.virtualFileSystem.VirtualFile
import consulo.virtualFileSystem.event.{BulkFileListener, VFileDeleteEvent, VFileEvent}
import consulo.application.util.concurrent.AppExecutorUtil
import org.jetbrains.bsp.settings.{BspProjectSettings, BspSettings}
import org.jetbrains.plugins.scala.project.ProjectExt

import java.util.concurrent.{ScheduledFuture, TimeUnit}

/**
  * Builds bsp modules on file save. We should change this to support the bsp file change notifications.
 * TODO IDEA platform already supports a save-triggered build-in-background mode. Investigate if we can replace this service.
  */
// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class BspBuildLoopService @Inject()(project: Project) {

  private def bspSettings: Option[BspProjectSettings] =
    Option(
      BspSettings
        .getInstance(project)
        .getLinkedProjectSettings(project.getBasePath)
    )

  private val busConnection: MessageBusConnection = project.getMessageBus.connect(project.unloadAwareDisposable)
  private val fileIndex = ProjectRootManager.getInstance(project).getFileIndex
  private val compilerManager = CompilerManager.getInstance(project)

  busConnection.subscribe(classOf[BulkFileListener], FileChangeListener)

  // upstream: FileChangeListenerBase
  private object FileChangeListener extends BulkFileListener {

    /** Nanoseconds to wait between checking stuff to compile */
    private val checkDelay = 30 * 1000000

    private val modulesToCompile = scala.collection.mutable.HashSet[Module]()
    private var changesSinceCompile = false
    private var lastChangeTimestamp: Long = 0

    /** Delays compilation just a little bit so that it's less likely that multiple builds are triggered for one
      * set of changes. */
    private var scheduledCompile: ScheduledFuture[?] =
      AppExecutorUtil.getAppScheduledExecutorService.schedule[Unit](()=>(), 0, TimeUnit.NANOSECONDS)

    private def checkCompile(): Unit = {
      val now = System.nanoTime()
      if ((now - lastChangeTimestamp) > checkDelay
      ) {
        scheduledCompile.cancel(false)
        runCompile()
      }
    }


    override def before(events: java.util.List[? <: VFileEvent]): Unit =
      events.forEach {
        case event: VFileDeleteEvent => deleteFile(event.getFile, event)
        case _ =>
      }

    override def after(events: java.util.List[? <: VFileEvent]): Unit = {
      events.forEach {
        case _: VFileDeleteEvent =>
        case event =>
          val file = event.getFile
          if (file != null) updateFile(file, event)
      }
      apply()
    }

    private def apply(): Unit = if (
      changesSinceCompile &&
        modulesToCompile.nonEmpty &&
        bspSettings.exists(_.buildOnSave) &&
        (scheduledCompile.isCancelled || scheduledCompile.isDone)
    ) {
      scheduledCompile = AppExecutorUtil.getAppScheduledExecutorService
        .scheduleWithFixedDelay(() => checkCompile(), checkDelay, checkDelay, TimeUnit.NANOSECONDS)
    }

    private def updateFile(file: VirtualFile, event: VFileEvent): Unit =
      fileChanged(file, event)
    private def deleteFile(file: VirtualFile, event: VFileEvent): Unit =
      fileChanged(file, event)

    private def fileChanged(file: VirtualFile, event: VFileEvent) = {
      if (isSupported(file)) {
        changesSinceCompile = true
        lastChangeTimestamp = System.nanoTime()
        val module = fileIndex.getModuleForFile(file)
        if (module != null)
          modulesToCompile.add(module)
      }
    }

    private def runCompile(): Unit = {
      changesSinceCompile = false

      // upstream: ProjectTaskManager.build(modules).onSuccess
      val clearOnSuccess: CompileStatusNotification = (aborted: Boolean, errors: Int, _: Int, _: CompileContext) =>
        if (errors > 0 || aborted) {
          // modules stay queued for recompile on next try
          // TODO only re-queue failed modules? requires information to be available in ProjectTaskResult
        } else {
          modulesToCompile.clear()
        }

      val runnable: Runnable = { () =>
        compilerManager.make(project, modulesToCompile.toArray, clearOnSuccess)
      }
      ApplicationManager.getApplication.invokeLater(runnable, ModalityState.nonModal())
    }

    // TODO should allow all bsp-compiled types, depending on build server compatibility
    private def isSupported(file: VirtualFile) = file.getExtension match {
      case "scala" => true
      case "java" => true
      case _ => false
    }
  }
}

object BspBuildLoopService {
  def getInstance(project: Project): BspBuildLoopService =
    project.getInstance(classOf[BspBuildLoopService])
}
