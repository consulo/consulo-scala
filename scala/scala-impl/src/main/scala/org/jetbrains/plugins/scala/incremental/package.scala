package org.jetbrains.plugins
package scala

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.document.FileDocumentManager
import consulo.language.psi.PsiDocumentManager
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nullable

package object incremental {
  private final val REASON = "Incremental highlighting"

  private[incremental] implicit class DaemonCodeAnalyzerExt(private val daemon: DaemonCodeAnalyzer) extends AnyVal {
    // com.intellij.codeInsight.daemon.impl.FileStatusMap.combineDirtyScopes
    def combineDirtyScopes(document: Document, scope: TextRange): Unit = {
      daemon.getFileStatusMap.markFileScopeDirty(document, scope, document.getTextLength, REASON)
    }

    // upstream: DaemonCodeAnalyzerImpl.stopProcess(toRestartAlarm = true, reason) (not public in Consulo),
    // restarting the file re-highlights the whole file instead of only the dirty scope
    def stopProcess(project: Project, document: Document): Unit = {
      val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
      if (file != null) daemon.restart(file)
    }
  }

  private[incremental] implicit class TextRangeExt(private val a: TextRange) extends AnyVal {
    def diff(b: TextRange): TextRange = {
      if (a.isEmpty || b.contains(a)) return TextRange.EMPTY_RANGE
      if (!b.intersects(a) || (b.getStartOffset > a.getStartOffset && b.getEndOffset < a.getEndOffset)) return a
      if (b.getStartOffset <= a.getStartOffset) new TextRange(b.getEndOffset, a.getEndOffset)
      else new TextRange(a.getStartOffset, b.getStartOffset)
    }
  }

  private[incremental] implicit class EditorExt(private val that: Editor) extends AnyVal {
    // Editor.getVirtualFile is obsolete and incompatible with Diff Viewer
    @Nullable def virtualFile: VirtualFile = FileDocumentManager.getInstance.getFile(that.getDocument)
  }

  private[incremental] def isScalaIn(@Nullable file: VirtualFile): Boolean =
    file != null && (file.getExtension == "scala" || file.getExtension == "sc" || file.getExtension == "sbt" || file.getExtension == "mill")
}
