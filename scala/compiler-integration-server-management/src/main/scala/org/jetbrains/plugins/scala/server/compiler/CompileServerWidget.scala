package org.jetbrains.plugins.scala.server.compiler

import consulo.platform.base.icon.PlatformIconGroup
import consulo.dataContext.DataManager
import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent, AnActionWithSyncUpdate, AnSeparator, DefaultActionGroup, DumbAwareAction}
import consulo.application.ApplicationManager
import consulo.component.messagebus.MessageBusConnection
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.ex.popup.JBPopupFactory
import consulo.project.ui.wm.{StatusBar, StatusBarWidget}
import consulo.ui.{Component => UIComponent}
import consulo.ui.event.{ClickEvent, ComponentEventListener}
import consulo.ui.ex.RelativePoint
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.image.Image
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.executeOnPooledThread

import java.awt.Point
import scala.concurrent.duration.Duration

private final class CompileServerWidget(project: Project) extends StatusBarWidget
  with StatusBarWidget.IconPresentation
  with CompileServerWidgetUpdateListener {

  private val connection: MessageBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  private var statusBar: StatusBar = scala.compiletime.uninitialized

  override def getId: String = CompileServerWidgetFactory.ID

  override def getPresentation: StatusBarWidget.WidgetPresentation = this

  override def install(statusBar: StatusBar): Unit = {
    this.statusBar = statusBar
    connection.subscribe(CompileServerWidgetUpdateListener.Topic, this)
  }

  override def dispose(): Unit = {}

  override def getIcon: Image = {
    if (launcher.running) CompileServerWidgetFactory.IconRunning
    else CompileServerWidgetFactory.IconStopped
  }

  override def getClickListener: ComponentEventListener[UIComponent, ClickEvent] =
    event => showPopup(event.getComponent)

  //noinspection ReferencePassedToNls
  override def getTooltipText: LocalizeValue = {
    val portDetail = launcher.compileServerPort.map(p => s"TCP $p")
    val pidDetail = launcher.pid.map(p => s"PID $p")
    val details = portDetail ++ pidDetail
    val detailsText = if (details.isEmpty) "" else details.mkString(" (", ", ", ")")
    LocalizeValue.of(title + detailsText)
  }

  private def showPopup(uiComponent: UIComponent): Unit = {
    val component = TargetAWT.to(uiComponent)
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val group = new DefaultActionGroup(Start, Stop, AnSeparator.getInstance, Configure)
    val context = DataManager.getInstance.getDataContext(component)
    val popup = JBPopupFactory.getInstance.createActionGroupPopup(title, group, context, mnemonics, true)
    val dimension = popup.getContent.getPreferredSize
    val at = new Point(0, -dimension.height)
    popup.show(new RelativePoint(component, at))
  }

  override def updateWidget(): Unit = {
    if (statusBar != null) {
      statusBar.updateWidget(getId)
    }
  }

  private def launcher: CompileServerLauncher.type = CompileServerLauncher

  @Nls
  private def title: String = ServerManagementBundle.message("scala.compile.server.title")

  private object Start extends DumbAwareAction(LocalizeValue.of(ServerManagementBundle.message("action.run")), LocalizeValue.of(ServerManagementBundle.message("start.compile.server")), PlatformIconGroup.actionsExecute()) with AnActionWithSyncUpdate {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(!launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      executeOnPooledThread(launcher.ensureServerRunning(project))

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private object Stop extends DumbAwareAction(LocalizeValue.of(ServerManagementBundle.message("action.stop")), LocalizeValue.of(ServerManagementBundle.message("shutdown.compile.server")), PlatformIconGroup.actionsSuspend()) with AnActionWithSyncUpdate {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      executeOnPooledThread(launcher.stopServerAndWaitFor(Duration.Zero))

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private object Configure extends DumbAwareAction(LocalizeValue.of(ServerManagementBundle.message("action.configure")), LocalizeValue.of(ServerManagementBundle.message("configure.compile.server")), PlatformIconGroup.generalSettings()) {
    override def actionPerformed(e: AnActionEvent): Unit =
      CompileServerSettingsUtil.showCompileServerSettingsDialog(project)
  }
}
