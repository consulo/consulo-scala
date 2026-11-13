package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.language.editor.intention.IntentionAction
import consulo.webBrowser.BrowserUtil
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment

object ReportHighlightingErrorQuickFix extends IntentionAction {

  override def getText: String = ScalaBundle.message("report.highlighting.error.fix")

  override def startInWriteAction: Boolean = false

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = {
    if (file.is[ScalaCodeFragment]) return false
    true
  }

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit =
    BrowserUtil.browse("https://youtrack.jetbrains.com/newIssue?project=SCL")

  override def getFamilyName: String = ScalaBundle.message("report.highlighting.error.fix")
}
