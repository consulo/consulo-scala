package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.application.Application
import consulo.language.editor.DaemonCodeAnalyzer
import consulo.language.psi.PsiManager
import consulo.module.content.layer.event.{ModuleRootEvent, ModuleRootListener}
import consulo.project.{Project, ProjectManager}
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.annotator.hints.AnnotatorHints
import org.jetbrains.plugins.scala.compiler.highlighting.services.BackgroundExecutorService.executeOnBackgroundThreadInNotDisposed
import org.jetbrains.plugins.scala.compiler.highlighting.services.{CompilationLogicService, ExternalHighlightersService}
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.EditorTrigger
import org.jetbrains.plugins.scala.extensions.{inReadAction, inWriteAction, invokeLater, invokeWhenSmart}
import org.jetbrains.plugins.scala.settings.{CompilerHighlightingListener, ScalaHighlightingMode}

/**
 * Ensures correct toggling between "standard" and "compiler-based" highlighting modes.
 * Toggling means that the value of
 * [[ScalaHighlightingMode.isShowErrorsFromCompilerEnabled]] was changed.
 */
private[highlighting] object ToggleHighlightingModeListener {
  def compileOrEraseHighlightings(project: Project, triggerSource: String): Unit =
    invokeWhenSmart(project) {
      executeOnBackgroundThreadInNotDisposed(project) {
        if (ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project)) {
          inReadAction(AnnotatorHints.clearIn(project))
          CompilationLogicService(project).invalidateAll()
        } else {
          ExternalHighlightersService(project).eraseAllHighlightings()
          CompilationLogicService(project).invalidateAll()
        }
        // upstream: also resets the compile server notifications
        invokeLater(Application.get().getNoneModalityState) {
          if (!project.isDisposed) {
            forceStandardHighlighting(project)
            if (ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project)) {
              EditorTrigger.triggerOnSelectedEditor(project, triggerSource)
            }
          }
        }
      }
    }

  // upstream: also clears the ResolveCache (dropped with the PSI caches in Consulo)
  private def forceStandardHighlighting(project: Project): Unit = inWriteAction {
    PsiManager.getInstance(project).dropResolveCaches()
    PsiManager.getInstance(project).dropPsiCaches()
    DaemonCodeAnalyzer.getInstance(project).restart("Restart because highlighting was toggled")
  }
}

// upstream: ToggleHighlightingModeListener.OnCompilerHighlightingChange, a project listener of an application topic
@TopicImpl(ComponentScope.APPLICATION)
final class ToggleHighlightingModeListenerOnCompilerHighlightingChange extends CompilerHighlightingListener {
  override def compilerHighlightingScala2Changed(enabled: Boolean): Unit =
    compileOrEraseHighlightings("highlighting toggled")

  override def compilerHighlightingScala3Changed(enabled: Boolean): Unit =
    compileOrEraseHighlightings("highlighting toggled")

  private def compileOrEraseHighlightings(triggerSource: String): Unit =
    if (!Application.get().isUnitTestMode) {
      ProjectManager.getInstance().getOpenProjects.foreach(ToggleHighlightingModeListener.compileOrEraseHighlightings(_, triggerSource))
    }
}

// upstream: ToggleHighlightingModeListener.OnModuleRootChange and CompilerHighlightingModuleRootListener
@TopicImpl(ComponentScope.PROJECT)
final class ToggleHighlightingModeListenerOnModuleRootChange @Inject()(project: Project) extends ModuleRootListener {
  override def rootsChanged(event: ModuleRootEvent): Unit =
    if (!project.isDisposed && !Application.get().isUnitTestMode) {
      ToggleHighlightingModeListener.compileOrEraseHighlightings(project, "module roots changed")
    }
}
