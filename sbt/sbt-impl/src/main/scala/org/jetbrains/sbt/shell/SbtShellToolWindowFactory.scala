package org.jetbrains.sbt.shell

import consulo.annotation.component.ExtensionImpl

import consulo.ui.ex.action.{ActionGroup, ActionManager}
import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.application.dumb.DumbAware
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.toolWindow.{ToolWindow, ToolWindowAnchor}
import consulo.project.ui.wm.ToolWindowFactory
import consulo.project.ui.wm.ToolWindowManager
import consulo.ui.ex.BadgeIconSupplier
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.image.Image
import consulo.ui.layout.DockLayout
import consulo.ui.ex.content.{Content, ContentFactory}
import org.jetbrains.annotations.{Nls, NotNull}
import org.jetbrains.plugins.scala.extensions.{invokeLater, schedulePeriodicTask}
import org.jetbrains.sbt.api.icons.Icons
import org.jetbrains.sbt.shell.SbtShellToolWindowFactory.Log
import org.jetbrains.sbt.{SbtBundle, SbtUtil, shell}

import java.awt.{Component, Container, FocusTraversalPolicy}
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

/**
  * Creates the sbt shell toolwindow, which is docked at the bottom of sbt projects.
  *
  */
@ExtensionImpl(id = "sbt-shell-toolwindow")
class SbtShellToolWindowFactory extends ToolWindowFactory with DumbAware {
  override def getId: String = SbtShellToolWindowFactory.ID

  override def getAnchor: ToolWindowAnchor = ToolWindowAnchor.BOTTOM

  override def getIcon: Image = Icons.SBT_SHELL_TOOL_WINDOW

  override def getDisplayName: LocalizeValue = LocalizeValue.of(SbtShellToolWindowFactory.Title)

  override def canCloseContents: Boolean = false

  override def isDoNotActivateOnStart: Boolean = true

  /**
   * @note usage of "badge is originally inspired by "Build" tool window
   *       (see [[com.intellij.build.BuildContentManagerImpl]])
   */
  private val ToolWindowIconSupplier = new BadgeIconSupplier(Icons.SBT_SHELL_TOOL_WINDOW)

  /**
   * NOTE: when a new project is created this returns false<br>
   * In this case [[org.jetbrains.sbt.project.ShowSbtShellAfterCreatingNewProject]] explicitly activates the tool window
   */
  override def shouldBeAvailable(project: Project): Boolean =
    SbtUtil.isSbtProject(project)

  // called once per project open
  // upstream: also adds the ctrl+shift+S shortcut of the "activate tool window" action to the default keymap
  @RequiredUIAccess
  override def init(project: Project, toolWindow: ToolWindow): Unit = {
    Log.debug("init")
    toolWindow.setDisplayName(LocalizeValue.of(SbtShellToolWindowFactory.Title))
    toolWindow.setIcon(ToolWindowIconSupplier.getOriginalIcon)
  }

  // called once per project open, is not called during sbt shell restart OR close/open etc...
  @RequiredUIAccess
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    Log.debug("createToolWindowContent")
    // focus sbt shell input when opening toolWindow with shortcut. #SCL-13225
    val defaultFocusPolicy = toolWindow.getComponent.getFocusTraversalPolicy
    val focusPolicy = new shell.SbtShellToolWindowFactory.TraversalPolicy(project, defaultFocusPolicy)
    toolWindow.getComponent.setFocusTraversalPolicy(focusPolicy)

    scheduleIconUpdate(project, toolWindow)

    SbtProcessManager.forProject(project).initAndRunAsync()
  }

  private def scheduleIconUpdate(project: Project, toolWindow: ToolWindow): Unit =
    schedulePeriodicTask(500L, TimeUnit.MILLISECONDS, toolWindow.getContentManager) {
      invokeLater {
        if (!project.isDisposed) {
          val isAlive = !project.isDisposed && SbtProcessManager.forProject(project).isAlive
          val icon = ToolWindowIconSupplier.getLiveIndicatorIcon(isAlive)
          toolWindow.setIcon(icon)
        }
      }
    }

}

object SbtShellToolWindowFactory {

  private val Log = Logger.getInstance(getClass)

  @Nls
  def Title: String = SbtBundle.message("sbt.shell.title")
  val ID = "sbt-shell-toolwindow"

  // TODO: we could pass ToolWindow directly to ProcessManager ans SbtShellRunner
  def instance(implicit project: Project): Option[ToolWindow] = {
    val result = for {
      manager <- Option(ToolWindowManager.getInstance(project))
      window <- Option(manager.getToolWindow(ID))
    } yield window

    if (result.isEmpty) {
      // upstream: returns a MockToolWindow in unit test mode
      if (ApplicationManager.getApplication.isUnitTestMode) {
        return None
      }

      Log.error(s"Failed to create sbt shell toolwindow content for $project.")
    }

    result
  }

  private class TraversalPolicy(project: Project, defaultPolicy: FocusTraversalPolicy) extends FocusTraversalPolicy {
    override def getComponentAfter(aContainer: Container, aComponent: Component): Component =
      defaultPolicy.getComponentAfter(aContainer, aComponent)

    override def getComponentBefore(aContainer: Container, aComponent: Component): Component =
      defaultPolicy.getComponentBefore(aContainer, aComponent)

    override def getFirstComponent(aContainer: Container): Component =
      defaultPolicy.getFirstComponent(aContainer)

    override def getLastComponent(aContainer: Container): Component =
      defaultPolicy.getLastComponent(aContainer)

    override def getDefaultComponent(aContainer: Container): Component = {
      // default implementation focuses the toolwindow frame, but we want the editor to be directly focused to edit it directly
      val sbtManager = SbtProcessManager.forProject(project)

      def getShellComponent: Option[JComponent] =
        for {
          shellRunner <- sbtManager.shellRunner
          view <- Option(shellRunner.getConsoleView)
          editor <- Option(view.getConsoleEditor)
        } yield editor.getContentComponent

      val shellComponent =
        if (!sbtManager.isAlive) None
        else getShellComponent

      shellComponent.getOrElse(defaultPolicy.getDefaultComponent(aContainer))
    }
  }

  private[shell] def initUi(
    project: Project,
    actionGroup: ActionGroup,
    component: => JComponent,
  ): Unit =
    SbtShellToolWindowFactory.instance(using project).foreach { toolWindow =>
      invokeLater {
        val content = createToolWindowContent(actionGroup, component, project.getName)
        setContent(toolWindow, content)
      }
    }

  private[shell] def setContent(toolWindow: ToolWindow, @NotNull content: Content): Unit = {
    Log.trace("setContent")
    val twContentManager = toolWindow.getContentManager
    twContentManager.removeAllContents(true)
    twContentManager.addContent(content)
  }

  @RequiredUIAccess
  private def createToolWindowContent(
    actionGroup: ActionGroup,
    component: JComponent,
    @Nls projectName: String
  ): Content = {
    Log.debug("createToolWindowContent")

    val actionToolBar = ActionManager.getInstance().createActionToolbar("sbt-shell-toolbar", actionGroup, false)

    val mainPanel = DockLayout.create()
    mainPanel.left(actionToolBar.getUIComponent)
    mainPanel.center(TargetAWT.wrap(component))
    actionToolBar.setTargetUIComponent(mainPanel)

    //noinspection ScalaExtractStringToBundle
    val content = ContentFactory.getInstance.createUIContent(mainPanel, "sbt-shell-toolwindow-content", true)
    content.setTabName(projectName)
    content.setDisplayName(projectName)
    content.setToolwindowTitle(projectName)

    content
  }
}
