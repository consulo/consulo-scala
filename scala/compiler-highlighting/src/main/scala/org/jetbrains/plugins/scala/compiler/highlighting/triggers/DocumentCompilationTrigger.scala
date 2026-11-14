package org.jetbrains.plugins.scala.compiler.highlighting.triggers

import consulo.fileEditor.FileEditorManager
import consulo.project.Project
import org.jetbrains.plugins.scala.compiler.highlighting.services.TriggerService

object DocumentCompilationTrigger:
  /**
   * Compiles every file open in a visible editor, e.g. after a build changed the module output they are compiled
   * against.
   *
   * upstream: compiles the eligible documents right away, within the ongoing incremental compilation or as separate
   * compilations after an IDE build. Here they are handed over to the scheduler, which checks the eligibility.
   */
  def triggerDocumentCompilationInAllOpenEditors(project: Project, debugReason: String): Unit =
    FileEditorManager.getInstance(project).getSelectedFiles.foreach { file =>
      TriggerService(project).bypass(file, s"[${file.getName}] $debugReason")
    }
