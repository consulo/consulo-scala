package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.compiler.CompileContext
import consulo.compiler.event.CompilationStatusListener
import consulo.project.Project
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.compiler.highlighting.services.BackgroundExecutorService.executeOnBackgroundThreadInNotDisposed
import org.jetbrains.plugins.scala.compiler.highlighting.services.CompilationLogicService
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.DocumentCompilationTrigger
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

/**
 * The module output the documents are compiled against changed: the open editors are compiled again.
 *
 * upstream: CompilerHighlightingBuildManagerListener (JPS BuildManagerListener, only after a successful build and only
 * for Scala 3); the build itself also reports its compiler messages to the highlighting through the compiler events
 */
@TopicImpl(ComponentScope.PROJECT)
final class CompilerHighlightingCompilationStatusListener @Inject()(project: Project) extends CompilationStatusListener {

  override def compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext): Unit =
    if (!aborted && !project.isDisposed && ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project)) {
      executeOnBackgroundThreadInNotDisposed(project) {
        CompilationLogicService(project).invalidateAll()
        DocumentCompilationTrigger.triggerDocumentCompilationInAllOpenEditors(project, "On Build Finish")
      }
    }
}
