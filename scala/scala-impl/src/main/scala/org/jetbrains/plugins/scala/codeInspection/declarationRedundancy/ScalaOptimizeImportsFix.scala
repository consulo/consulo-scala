package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.java.analysis.impl.localize.JavaQuickFixLocalize
import consulo.language.editor.localize.CodeInsightLocalize
import consulo.language.editor.refactoring.ImportOptimizer
import consulo.localize.LocalizeValue
import consulo.project.DumbService
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import consulo.language.editor.intention.{HighPriorityAction, IntentionAction, LowPriorityAction}
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile
import consulo.fileEditor.util.FileContentUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions.{PsiFileExt, executeWriteActionCommand, inReadAction}
import consulo.project.Project
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import scala.jdk.CollectionConverters._

sealed abstract class ScalaOptimizeImportsFixBase extends IntentionAction {

  override final def startInWriteAction: Boolean = false

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    file.hasScalaPsi

  /**
   * OptimizeImportsProcessor will select a suitable import optimizer, handling Play2 templates
   *
   * @param file Any parallel psi file
   */
  // upstream: new OptimizeImportsProcessor(project, file).run() (internal in Consulo)
  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    if (DumbService.isDumb(project)) return

    val runnables = inReadAction {
      for {
        optimizer <- ImportOptimizer.forFile(file).asScala.toSeq
        psiFile <- file.getViewProvider.getAllFiles.asScala
        if optimizer.supports(psiFile)
      } yield optimizer.processFile(psiFile)
    }
    if (runnables.nonEmpty)
      executeWriteActionCommand(CodeInsightLocalize.processOptimizeImports().get)(runnables.foreach(_.run()))(using project)
  }
}

final class ScalaOptimizeImportsFix extends ScalaOptimizeImportsFixBase with HighPriorityAction {

  override def getText: LocalizeValue = JavaQuickFixLocalize.optimizeImportsFix()

  def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo =
    IntentionPreviewInfo.EMPTY
}

final class ScalaEnableOptimizeImportsOnTheFlyFix extends ScalaOptimizeImportsFixBase {

  override def getText: LocalizeValue = JavaQuickFixLocalize.enableOptimizeImportsOnTheFly()

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    !ScalaApplicationSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY &&
      super.isAvailable(project, editor, file)

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    ScalaApplicationSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY = true
    super.invoke(project, editor, file)
  }

  def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo =
    IntentionPreviewInfo.EMPTY
}

final class MarkImportAsAlwaysUsed(importText: String) extends IntentionAction with LowPriorityAction {

  override def getText: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("mark.import.as.always.used.in.this.project"))

  override def startInWriteAction: Boolean = true

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = {
    importText.contains(".") && !ScalaCodeStyleSettings.getInstance(project).isAlwaysUsedImport(importText)
  }

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    val settings = ScalaCodeStyleSettings.getInstance(project)
    settings.setAlwaysUsedImports((settings.getAlwaysUsedImports ++ Array(importText)).sorted)
    FileContentUtil.reparseFiles(project, Seq(file.getVirtualFile).asJava, true)
  }

  def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = {
    val settings = ScalaCodeStyleSettings.getInstance(project)
    val updatedList: java.util.List[String] = (settings.getAlwaysUsedImports ++ Array(importText)).sorted.toList.asJava
    IntentionPreviewInfo.addListOption(updatedList, importText,
      ScalaBundle.message("imports.panel.imports.always.marked.as.used"))
  }
}
