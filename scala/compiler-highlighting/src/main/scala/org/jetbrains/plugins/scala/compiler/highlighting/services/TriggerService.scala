package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.{ComponentScope, ServiceAPI}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nullable

/**
 * Main entrypoint to handle any platform event
 */
@ServiceAPI(ComponentScope.PROJECT)
trait TriggerService:
  /**
   * Hands the file over once the events about it stop.
   *
   * Use for high-frequency events such as document edits, where each one makes the last redundant. Each
   * file has its own window, so a file being written in the background cannot hold up the one being
   * typed in.
   */
  def schedule(@Nullable file: VirtualFile, reason: String): Unit

  /**
   * Hands the file over at once, cancelling any window pending for it.
   *
   * Use for low-frequency events such as tab switches, which are unlikely to be followed by another event
   * about the same file and where the user is waiting to see the result.
   */
  def bypass(@Nullable file: VirtualFile, reason: String): Unit

object TriggerService:
  def apply(project: Project): TriggerService = project.getInstance(classOf[TriggerService])
