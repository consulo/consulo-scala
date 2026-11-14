package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Singleton
import org.jetbrains.plugins.scala.compiler.highlighting.core.CompilerGeneratedStateManager

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

/**
 * Answers the one question the triggers ask: whether the diagnostics recorded for a file still describe it, or it
 * has to be compiled.
 *
 * upstream: owns a model of the JPS/BSP build (unbuilt files, built targets, failed and blocked targets) and decides
 * between an incremental build of the unbuilt files and a document compilation. There is no build of our own in
 * Consulo, every compilation is a document compilation against the module output, so only "recorded" is kept:
 * the last compilation of the file happened after the last change of any source and the file text is the compiled one.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class CompilationLogicService {

  /** Increments on every change which may change the diagnostics of any file */
  private val changesEpoch = new AtomicLong(0L)

  /** The epoch each file was last compiled at */
  private val compiledAt = new AtomicReference(Map.empty[VirtualFile, Long])

  /** Records that `file` no longer matches what was compiled from it. */
  def fileModified(file: VirtualFile): Unit = filesModified(Set(file))

  /** Records that `files` no longer match what was compiled from them. */
  def filesModified(files: Set[VirtualFile]): Unit =
    if (files.nonEmpty) changesEpoch.incrementAndGet()

  /** Records that no recorded diagnostics can be relied upon, e.g. after a change of roots, libraries or SDK, or a build. */
  def invalidateAll(): Unit = {
    changesEpoch.incrementAndGet()
    compiledAt.set(Map.empty)
  }

  /** Snapshots the state a compilation is about to run against */
  def compilationStarted(): Long = changesEpoch.get()

  /** Records a compilation of `files` which started at `token` and was not interrupted */
  def compilationFinished(token: Long, files: Set[VirtualFile]): Unit = {
    compiledAt.updateAndGet(current => current.filter(_._1.isValid) ++ files.map(_ -> token))
    ()
  }

  /** Whether the diagnostics recorded for `file` still describe it, no compilation is needed to show them */
  def isRecorded(project: Project, file: VirtualFile): Boolean =
    compiledAt.get().get(file).contains(changesEpoch.get()) &&
      CompilerGeneratedStateManager.get(project).toHighlightingState.describesCurrentText(file)
}

object CompilationLogicService {
  def apply(project: Project): CompilationLogicService =
    project.getInstance(classOf[CompilationLogicService])
}
