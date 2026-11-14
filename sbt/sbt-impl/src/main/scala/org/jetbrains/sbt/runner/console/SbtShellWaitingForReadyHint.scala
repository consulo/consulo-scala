package org.jetbrains.sbt.runner.console

import consulo.colorScheme.TextAttributes
import consulo.execution.ui.console.{ConsoleView, ConsoleViewContentType, HyperlinkInfo}
import consulo.project.Project
import consulo.project.ui.wm.ToolWindowManager
import consulo.ui.ex.SimpleTextAttributes
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
 * The waiting text itself is printed as normal console output with a custom grey italic content type.
 * upstream: the clickable "Open sbt shell" part is a console inlay (InlayProvider) added by a message filter;
 * Consulo consoles have no inlay providers, so it is printed as a hyperlink.
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

  def print(consoleView: ConsoleView): Unit = invokeAndWait {
    printGrayItalicStatusWithActionHyperlink(consoleView)
  }

  private def printGrayItalicStatusWithActionHyperlink(consoleView: ConsoleView): Unit = {
    consoleView.print(HintText, GrayItalicStatusContentType)
    consoleView.print(" ", GrayItalicStatusContentType)
    consoleView.printHyperlink(OpenShellText, (project => activateSbtShell(project)): HyperlinkInfo)
    consoleView.print(System.lineSeparator(), GrayItalicStatusContentType)
  }

  private def activateSbtShell(project: Project): Unit = invokeLater {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SbtShellToolWindowFactory.ID)
    if (toolWindow != null) {
      toolWindow.activate(null, true)
    }
  }
}