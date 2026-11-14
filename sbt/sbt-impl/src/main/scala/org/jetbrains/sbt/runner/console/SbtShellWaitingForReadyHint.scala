package org.jetbrains.sbt.runner.console

import consulo.codeEditor.Editor
import consulo.codeEditor.event.{EditorMouseEvent, EditorMouseListener}
import consulo.colorScheme.TextAttributes
import consulo.execution.ui.console.{ConsoleView, ConsoleViewContentType, HyperlinkInfo}
import consulo.ide.impl.idea.codeInsight.hints.HintRenderer
import consulo.project.Project
import consulo.project.ui.wm.ToolWindowManager
import consulo.ui.ex.SimpleTextAttributes
import consulo.util.dataholder.Key
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.{invokeAndWait, invokeLater}
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.shell.SbtShellToolWindowFactory

import java.awt.Font

/**
 * Displays a grey hint in the Run/Debug console when it's waiting for SBT shell to become ready:
 * ```text
 * Waiting for sbt shell to become ready... (Open sbt shell)
 * ```
 *
 * ## High-level implementation overview:
 * 1. The waiting text itself is printed as normal console output with a custom grey italic content type.
 * 2. The clickable "Open sbt shell" part is added separately as an inlay at the end of the printed waiting line.
 *
 * upstream: the inlay is contributed by a console message filter (Filter.ResultItem with InlayProvider) - Consulo filters
 * have no inlay providers, so the inlay is added to the console editor directly once the output is flushed.
 * Consoles without an editor get a hyperlink instead.
 */
private[runner] object SbtShellWaitingForReadyHint {
  @Nls
  // upstream: @VisibleForTesting
  val HintText: String = SbtBundle.message("waiting.for.sbt.shell.to.become.ready")

  @Nls
  // upstream: @VisibleForTesting
  val OpenShellText: String = SbtBundle.message("open.sbt.shell")

  private val GrayItalicStatusContentType: ConsoleViewContentType = {
    val attributes = new TextAttributes()
    attributes.setForegroundColor(SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES.foreground())
    attributes.setFontType(Font.ITALIC)
    new ConsoleViewContentType("SBT_SHELL_WAITING_HINT_GRAY_ITALIC_STATUS", attributes)
  }

  private val ClickListenerInstalledKey: Key[java.lang.Boolean] =
    Key.create("SbtShellWaitingForReadyHint.clickListenerInstalled")

  def print(consoleView: ConsoleView): Unit = invokeAndWait {
    // the console editor is created together with the component
    consoleView.getComponent
    if (consoleView.getEditor != null) printGrayItalicStatusWithInlineActionHint(consoleView)
    else printGrayItalicStatusWithActionHyperlink(consoleView)
  }

  private def printGrayItalicStatusWithInlineActionHint(consoleView: ConsoleView): Unit = {
    consoleView.print(HintText, GrayItalicStatusContentType)
    consoleView.print(System.lineSeparator(), GrayItalicStatusContentType)
    consoleView.performWhenNoDeferredOutput(() => addInlineActionHint(consoleView))
  }

  private def printGrayItalicStatusWithActionHyperlink(consoleView: ConsoleView): Unit = {
    consoleView.print(HintText, GrayItalicStatusContentType)
    consoleView.print(" ", GrayItalicStatusContentType)
    consoleView.printHyperlink(OpenShellText, (project => activateSbtShell(project)): HyperlinkInfo)
    consoleView.print(System.lineSeparator(), GrayItalicStatusContentType)
  }

  private def addInlineActionHint(consoleView: ConsoleView): Unit = {
    val editor = consoleView.getEditor
    if (editor == null || editor.isDisposed) return

    val index = editor.getDocument.getImmutableCharSequence.toString.lastIndexOf(HintText)
    if (index >= 0) {
      installClickListenerIfNeeded(editor)
      editor.getInlayModel.addInlineElement(index + HintText.length, true, new OpenSbtShellHintRenderer)
    }
  }

  private def installClickListenerIfNeeded(editor: Editor): Unit =
    if (editor.getUserData(ClickListenerInstalledKey) != java.lang.Boolean.TRUE) {
      editor.putUserData(ClickListenerInstalledKey, java.lang.Boolean.TRUE)
      editor.addEditorMouseListener(new EditorMouseListener {
        override def mouseClicked(event: EditorMouseEvent): Unit = {
          val inlay = event.getInlay
          if (inlay != null && inlay.getRenderer.isInstanceOf[OpenSbtShellHintRenderer]) {
            Option(editor.getProject).foreach(activateSbtShell)
            event.consume()
          }
        }
      })
    }

  private def activateSbtShell(project: Project): Unit = invokeLater {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SbtShellToolWindowFactory.ID)
    if (toolWindow != null) {
      toolWindow.activate(null, true)
    }
  }

  // upstream: a rounded small text presentation (PresentationFactory.roundWithBackground) with a reference on hover
  private final class OpenSbtShellHintRenderer extends HintRenderer(OpenShellText)
}
