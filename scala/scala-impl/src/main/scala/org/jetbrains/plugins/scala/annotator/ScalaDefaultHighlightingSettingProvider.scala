package org.jetbrains.plugins.scala.annotator

import consulo.language.editor.{DefaultHighlightingSettingProvider, FileHighlightingSetting}
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.application.util.registry.Registry
import consulo.virtualFileSystem.VirtualFile
import com.intellij.java.language.psi.PsiJavaFile
import consulo.language.psi.PsiManager
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

class ScalaDefaultHighlightingSettingProvider extends DefaultHighlightingSettingProvider {

  /** used e.g. in [[com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile.getDefaultHighlightingSetting]] */
  override def getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting = {
    if (ApplicationManager.getApplication.isUnitTestMode)
      null
    else if (!ScalaHighlightingMode.showCompilerErrorsScala3(project))
      null
    else if (!Registry.is("scala.compiler.highlighting.suppress.java.highlighting")) null
    else if (file.isValid) {
      val psiManager = PsiManager.getInstance(project)
      val psiFile = psiManager.findFile(file)
      psiFile match {
        case file: PsiJavaFile if file.isInScala3Module =>
          FileHighlightingSetting.SKIP_HIGHLIGHTING
        case _ =>
          null
      }
    }
    else null
  }
}
