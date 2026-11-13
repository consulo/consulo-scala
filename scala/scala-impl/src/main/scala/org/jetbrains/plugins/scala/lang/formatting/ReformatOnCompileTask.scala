package org.jetbrains.plugins.scala.lang.formatting

import consulo.annotation.component.ExtensionImpl

import consulo.language.codeStyle.CodeStyle
import consulo.undoRedo.CommandProcessor
import consulo.compiler.CompileContext
import consulo.compiler.scope.CompileScope
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.LocalFileSystem
import consulo.language.codeStyle.CodeStyleManager
import consulo.language.psi.{PsiFile, PsiManager}
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtDynamicConfigService
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.processors.ScalaFmtPreFormatProcessor
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.util.compile.ScalaCompileTask

import scala.jdk.CollectionConverters._

@ExtensionImpl
final class ReformatOnCompileTask(project: Project) extends ScalaCompileTask {

  override protected def run(context: CompileContext): Boolean = {
    val scalaSettings = codeStyleSettings
    if (scalaSettings.REFORMAT_ON_COMPILE) {
      ScalaFmtPreFormatProcessor.inFailSilentMode {
        reformatScopeFiles(context.getCompileScope, scalaSettings)
      }
    }
    true
  }

  override protected def presentableName: String = "Reformatting Scala sources on compile"

  override protected def log: Logger = ReformatOnCompileTask.Log

  private def codeStyleSettings: ScalaCodeStyleSettings =
    CodeStyle.getSettings(project).getCustomSettings(classOf[ScalaCodeStyleSettings])

  private def reformatScopeFiles(compileScope: CompileScope, scalaSettings: ScalaCodeStyleSettings): Unit = for {
    path <- compileScope.getFiles(ScalaFileType.INSTANCE).asScala
    virtualFile <- Option(LocalFileSystem.getInstance.findFileByNioFile(path))
    psiFile = inReadAction(PsiManager.getInstance(project).findFile(virtualFile))
    if shouldFormatFile(psiFile, scalaSettings)
    psiFile <- psiFile.asOptionOf[ScalaFile].filterNot(_.isWorksheetFile)
  } {
    invokeAndWait { () =>
      CommandProcessor.getInstance().runUndoTransparentAction { () =>
        CodeStyleManager.getInstance(project).reformat(psiFile)
      }
    }
  }

  private def shouldFormatFile(file: PsiFile, scalaSettings: ScalaCodeStyleSettings): Boolean = {
    if (scalaSettings.USE_SCALAFMT_FORMATTER()) {
      ScalafmtDynamicConfigService.isIncludedInProject(file)
    } else {
      true
    }
  }
}

private object ReformatOnCompileTask {
  private val Log: Logger = Logger.getInstance(classOf[ReformatOnCompileTask])
}
