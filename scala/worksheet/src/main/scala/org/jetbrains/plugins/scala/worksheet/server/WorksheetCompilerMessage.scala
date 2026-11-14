package org.jetbrains.plugins.scala.worksheet.server

import consulo.compiler.{CompilerMessage, CompilerMessageCategory}
import consulo.localize.LocalizeValue
import consulo.navigation.{Navigatable, OpenFileDescriptorFactory}
import consulo.project.Project
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel
import consulo.virtualFileSystem.VirtualFile

/**
 * upstream: com.intellij.compiler.CompilerMessageImpl (internal in Consulo)
 *
 * @param line   1-based, -1 if not available
 * @param column 1-based, -1 if not available
 */
final case class WorksheetCompilerMessage(
  project: Project,
  category: CompilerMessageCategory,
  text: String,
  file: VirtualFile,
  line: Int,
  column: Int
) extends CompilerMessage {

  override def getCategory: CompilerMessageCategory = category

  override def getMessage: LocalizeValue = LocalizeValue.of(text)

  override def getNavigatable: Navigatable =
    if (file == null || !file.isValid || line < 1) null
    else OpenFileDescriptorFactory.getInstance(project).newBuilder(file).line(line - 1).column(Math.max(0, column - 1)).build()

  override def getUrl: String = if (file == null) null else file.getUrl

  override def getExportTextPrefix: String = NewErrorTreeViewPanel.createExportPrefix(line)

  override def getRenderTextPrefix: String = NewErrorTreeViewPanel.createRendererPrefix(line, column)

  override def getLine: Int = line

  override def getColumn: Int = column
}
