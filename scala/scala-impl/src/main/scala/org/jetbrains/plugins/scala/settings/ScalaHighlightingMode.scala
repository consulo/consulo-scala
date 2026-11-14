package org.jetbrains.plugins.scala.settings

import consulo.language.editor.highlight.HighlightingLevelManager
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.application.util.registry.Registry
import consulo.language.psi.{PsiElement, PsiFile}
import com.intellij.java.language.psi.PsiJavaFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.extensions.PsiFileExt
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.project.{ProjectExt, ProjectPsiElementExt}

import scala.concurrent.duration._

object ScalaHighlightingMode {

  @TestOnly
  private[scala] var compilerHighlightingEnabledInTests: Boolean = false

  private def isInTestMode: Boolean = ApplicationManager.getApplication.isUnitTestMode

  // compiler based highlighting (upstream compiler-highlighting module) is not ported yet: without it the errors would
  // not be shown at all, so the built-in highlighting is used regardless of the project settings
  private val compilerHighlightingAvailable = false

  def isCompilerHighlightingAvailable: Boolean = compilerHighlightingAvailable

  private def showCompilerErrorsScala2(project: Project): Boolean =
    compilerHighlightingEnabledInTests ||
      compilerHighlightingAvailable && !isInTestMode && ScalaProjectSettings.getInstance(project).isCompilerHighlightingScala2

  def showCompilerErrorsScala3(project: Project): Boolean =
    compilerHighlightingEnabledInTests ||
      compilerHighlightingAvailable && !isInTestMode && ScalaProjectSettings.getInstance(project).isCompilerHighlightingScala3

  /**
   * Whether the Scala 3 compiler reports the errors of the element's file, so the checks it covers can be skipped.
   * False for files left to the annotator, see [[isLeftToAnnotator]].
   */
  def showCompilerErrorsScala3(element: PsiElement): Boolean =
    showCompilerErrorsScala3(element.getProject) && element.isInScala3Module &&
      !Option(element.getContainingFile).exists(isLeftToAnnotator)

  /**
   * Mill build files are left to the annotator, like sbt build files: Mill rewrites them before compiling them, so they
   * cannot be compiled as written, and its build server never clears the errors it reports on them.
   */
  private def isLeftToAnnotator(file: PsiFile): Boolean = file match {
    case scalaFile: ScalaFile => scalaFile.isMillFile
    case _                    => false
  }

  /**
   * Should only be used in situations where there is no reference to a [[com.intellij.psi.PsiFile]] instance. The other
   * method that takes in a PsiFile instance as an argument should be preferred, as it does the same checks, but also
   * takes into consideration the Scala language version.
   */
  //noinspection ApiStatus,UnstableApiUsage
  def isShowErrorsFromCompilerEnabled(project: Project): Boolean =
    // upstream: TrustedProjects.isProjectTrusted(project) && ..., Consulo has no trusted projects
    showCompilerErrorsScala3(project) && project.hasScala3 ||
      showCompilerErrorsScala2(project) && project.hasScala

  def isShowErrorsFromCompilerEnabled(file: PsiFile): Boolean =
    file match {
      case scalaFile: ScalaFile => isShowErrorsFromCompilerEnabled(scalaFile)
      case javaFile: PsiJavaFile => isShowErrorsFromCompilerEnabled(javaFile)
      case _ => false
    }

  // upstream: HighlightingSettingsPerFile level is ESSENTIAL or FORCE_HIGHLIGHTING
  private[scala] def shouldHighlightBasedOnFileLevel(psiFile: PsiFile, project: Project): Boolean =
    HighlightingLevelManager.getInstance(project).shouldInspect(psiFile)

  private def isShowErrorsFromCompilerEnabled(file: ScalaFile): Boolean = {
    val virtualFile = file match {
      case ScFile.VirtualFile(vFile) => vFile
      case _                         => return false
    }

    val project = file.getProject
    val isRegularScalaFile = virtualFile.getFileType == ScalaFileType.INSTANCE
    if (isLeftToAnnotator(file)) {
      false
    } else if (isRegularScalaFile) {
      file.isScala3File && showCompilerErrorsScala3(project) ||
        file.isScala2File && showCompilerErrorsScala2(project)
    } else if (file.isWorksheetFile) {
      // actually this should work for regular files as well
      file.isInScala3Module && showCompilerErrorsScala3(project) ||
        file.isInScalaModule && showCompilerErrorsScala2(project)
    } else {
      false
    }
  }

  private def isShowErrorsFromCompilerEnabled(javaFile: PsiJavaFile): Boolean =
    showCompilerErrorsScala3(javaFile.getProject) && javaFile.isInScala3Module

  private def nonNegativeDuration(key: String, defaultValue: Int): FiniteDuration =
    math.max(Registry.intValue(key, defaultValue), 0).millis

  def compilationDelay(project: Project): FiniteDuration =
    ScalaProjectSettings.in(project).getCompilerHighlightingDelay.millis

  def compilationTimeoutToShowProgress: FiniteDuration =
    nonNegativeDuration("scala.highlighting.compilation.timeout.to.show.progress.millis", 7000)

  def useCompilerRanges: Boolean = Registry.is("scala.compiler.highlighting.use.compiler.ranges", true)
}
