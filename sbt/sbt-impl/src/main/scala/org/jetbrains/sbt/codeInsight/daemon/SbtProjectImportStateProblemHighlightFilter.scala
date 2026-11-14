package org.jetbrains.sbt.codeInsight.daemon

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.ProblemHighlightFilter
import com.intellij.java.language.impl.JavaFileType
import consulo.application.ApplicationManager
import consulo.virtualFileSystem.fileType.FileType
import consulo.java.impl.util.JavaProjectRootsUtil
import consulo.language.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.sbt.project.SbtProjectImportStateService

@ExtensionImpl
private final class SbtProjectImportStateProblemHighlightFilter extends ProblemHighlightFilter {
  import SbtProjectImportStateProblemHighlightFilter.{isQodanaTestExclusionEnabled, isTrackedFileType, isWritableSourceFile}

  override def shouldHighlight(psiFile: PsiFile): Boolean = {
    // If the source has a file type which we do not track, this filter does not decide whether the file should be highlighted.
    if (!isTrackedFileType(psiFile.getFileType)) return true

    val project = psiFile.getProject

    // If the sbt project is fully imported, the files should be highlighted.
    if (SbtProjectImportStateService.instance(project).isImported(psiFile)) return true

    // If the sbt project is not imported, disable highlighting for source files.
    val isSource = isWritableSourceFile(psiFile)
    !isSource || isQodanaTestExclusionEnabled
  }
}

object SbtProjectImportStateProblemHighlightFilter {
  // Currently interested only in Scala, Java, Kotlin and Groovy sources.
  private[sbt] def isTrackedFileType(fileType: FileType): Boolean = fileType match {
    case _: ScalaFileType => true
    case _: JavaFileType => true
    case ft => trackedFileExtensions.contains(ft.getDefaultExtension)
  }

  private[sbt] def isWritableSourceFile(psiFile: PsiFile): Boolean = {
    val virtualFile = psiFile.getVirtualFile
    virtualFile != null && virtualFile.isWritable && !JavaProjectRootsUtil.isOutsideJavaSourceRoot(psiFile)
  }

  // Ideally, this would hold references to each of the file types, but we're trying to avoid dependencies on
  // other plugins.
  private def trackedFileExtensions: Array[String] =
    Array("sc", "kt", "kts", "groovy")

  /**
   * Provided specifically to make the Qodana coverage tests pass.
   */
  private var qodanaTestExclusionEnabled: Boolean = false

  private def isQodanaTestExclusionEnabled: Boolean =
    ApplicationManager.getApplication.isUnitTestMode && qodanaTestExclusionEnabled

  // upstream: @ApiStatus.Internal
  @TestOnly
  private[jetbrains] def setQodanaTestExclusionEnabled(enabled: Boolean): Unit = {
    qodanaTestExclusionEnabled = enabled
  }
}
