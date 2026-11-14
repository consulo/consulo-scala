package org.jetbrains.plugins.scala.compiler.highlighting.triggers

import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.services.TriggerService

private[highlighting] object FileTrigger:
  def trigger(project: Project,  virtualFile: VirtualFile, reason: String): Unit =
    val debugReason = s"[${virtualFile.getPresentableName} content changed]: $reason"  
    TriggerService(project).schedule(virtualFile, debugReason)
