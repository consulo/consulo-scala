package org.jetbrains.plugins.scala.server.compiler
import consulo.application.AllIcons
import consulo.dataContext.DataManager
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, DefaultActionGroup}
import com.intellij.openapi.actionSystem.Separator
import consulo.application.ApplicationManager
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.ui.ex.popup.JBPopupFactory
import consulo.project.ui.wm.{StatusBar, StatusBarWidget}
import consulo.ui.ex.RelativePoint
import java.util.function.Consumer
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.executeOnPooledThread

import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import scala.concurrent.duration.Duration

private final class CompileServerWidget(project: Project) extends StatusBarWidget
  with StatusBarWidget.IconPresentation
  with Consumer[MouseEvent]
  with CompileServerWidgetFactory.UpdateWidgetListener {

  private val connection: MessageBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  private var statusBar: StatusBar = scala.compiletime.uninitialized

  override def ID(): String = CompileServerWidgetFactory.ID

  override def getPresentation: StatusBarWidget.WidgetPresentation = this

  override def install(statusBar: StatusBar): Unit = {
    this.statusBar = statusBar
    connection.subscribe(CompileServerWidgetFactory.Topic, this)
  }

  override def dispose(): Unit = {}

  override def getIcon: Icon = {
    if (launcher.running) CompileServerWidgetFactory.IconRunning
    else CompileServerWidgetFactory.IconStopped
  }

  override def getClickConsumer: Consumer[MouseEvent] = this

  //noinspection ReferencePassedToNls
  override def getTooltipText: String = {
    val portDetail = launcher.compileServerPort.map(p => s"TCP $p")
    val pidDetail = launcher.pid.map(p => s"PID $p")
    val details = portDetail ++ pidDetail
    val detailsText = if (details.isEmpty) "" else details.mkString(" (", ", ", ")")
    title + detailsText
  }

  override def consume(e: MouseEvent): Unit = {
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val group = new DefaultActionGroup(Start, Stop, Separator.getInstance, Configure)
    val context = DataManager.getInstance.getDataContext(e.getComponent)
    val popup = JBPopupFactory.getInstance.createActionGroupPopup(title, group, context, mnemonics, true)
    val dimension = popup.getContent.getPreferredSize
    val at = new Point(0, -dimension.height)
    popup.show(new RelativePoint(e.getComponent, at))
  }

  override def updateWidget(): Unit = {
    if (statusBar != null) {
      statusBar.updateWidget(ID())
    }
  }

  private def launcher: CompileServerLauncher.type = CompileServerLauncher

  @Nls
  private def title: String = ServerManagementBundle.message("scala.compile.server.title")

  private object Start extends AnAction(ServerManagementBundle.message("action.run"), ServerManagementBundle.message("start.compile.server"), AllIcons.Actions.Execute) with DumbAware {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(!launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      executeOnPooledThread(launcher.ensureServerRunning(project))

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private object Stop extends AnAction(ServerManagementBundle.message("action.stop"), ServerManagementBundle.message("shutdown.compile.server"), AllIcons.Actions.Suspend) with DumbAware {
    override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(launcher.running)

    override def actionPerformed(e: AnActionEvent): Unit =
      executeOnPooledThread(launcher.stopServerAndWaitFor(Duration.Zero))

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private object Configure extends AnAction(ServerManagementBundle.message("action.configure"), ServerManagementBundle.message("configure.compile.server"), AllIcons.General.Settings) with DumbAware {
    override def actionPerformed(e: AnActionEvent): Unit =
      CompileServerSettingsUtil.showCompileServerSettingsDialog(project)

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }
}
