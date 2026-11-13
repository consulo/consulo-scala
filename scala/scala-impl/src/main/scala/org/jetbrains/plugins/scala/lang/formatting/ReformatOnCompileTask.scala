package org.jetbrains.plugins.scala.lang.formatting

import consulo.annotation.component.ExtensionImpl

import consulo.language.codeStyle.CodeStyle
import consulo.undoRedo.CommandProcessor
import consulo.compiler.{BeforeCompileTask, CompileContext}
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
final class ReformatOnCompileTask extends ScalaCompileTask with BeforeCompileTask {

  // Consulo: BeforeCompileTask is application-level, the project comes from the compile context
  override protected def run(context: CompileContext): Boolean = {
    val project = context.getProject
    val scalaSettings = codeStyleSettings(project)
    if (scalaSettings.REFORMAT_ON_COMPILE) {
      ScalaFmtPreFormatProcessor.inFailSilentMode {
        reformatScopeFiles(project, context.getCompileScope, scalaSettings)
      }
    }
    true
  }

  override protected def presentableName: String = "Reformatting Scala sources on compile"

  override protected def log: Logger = ReformatOnCompileTask.Log

  private def codeStyleSettings(project: Project): ScalaCodeStyleSettings =
    CodeStyle.getSettings(project).getCustomSettings(classOf[ScalaCodeStyleSettings])

  private def reformatScopeFiles(project: Project, compileScope: CompileScope, scalaSettings: ScalaCodeStyleSettings): Unit = for {
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
