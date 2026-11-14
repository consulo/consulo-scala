package org.jetbrains.bsp

import consulo.platform.base.icon.PlatformIconGroup
import consulo.dataContext.DataManager
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate, AnSeparator, DefaultActionGroup}
import consulo.application.ApplicationManager
import consulo.application.dumb.DumbAware
import consulo.component.messagebus.MessageBusConnection
import consulo.localize.LocalizeValue
import consulo.ui.ex.popup.JBPopupFactory
import consulo.project.ui.wm.{StatusBar, StatusBarWidget}
import consulo.ui.{Component => UIComponent}
import consulo.ui.event.{ClickEvent, ComponentEventListener}
import consulo.ui.ex.RelativePoint
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.image.Image
import org.jetbrains.bsp.protocol.{BspCommunication, BspCommunicationService}
import org.jetbrains.bsp.settings.BspProjectSettings.BspServerConfig

import java.awt.Point
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

private final class BspServerWidget extends StatusBarWidget
  with StatusBarWidget.IconPresentation
  with BspServerWidgetUpdateListener {

  private val connection: MessageBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  private var statusBar: StatusBar = scala.compiletime.uninitialized

  override def getId: String = BspServerWidgetFactory.ID

  override def install(statusBar: StatusBar): Unit = {
    this.statusBar = statusBar
    connection.subscribe(BspServerWidgetFactory.Topic, this)
  }

  override def dispose(): Unit = {}

  override def getPresentation: StatusBarWidget.WidgetPresentation = this

  override def getIcon: Image =
    if (connectionsActive) BspServerWidgetFactory.IconRunning
    else BspServerWidgetFactory.IconStopped

  override def getTooltipText: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.widget.bsp.connection"))

  override def getClickListener: ComponentEventListener[UIComponent, ClickEvent] =
    event => showPopup(TargetAWT.to(event.getComponent))

  private def showPopup(component: java.awt.Component): Unit = {
    val openCom = BspCommunicationService.getInstance.listOpenComms
    val connectionClosers =
      (openCom.map(c => new CloseBspSession(c._1, c._2)).toList
        ++ List(new CloseAllSessions)
        ++ openCom.map(c => new TerminateServer(c._1, c._2)).toList
        :+ AnSeparator.getInstance
        ).asJava

    val group = new DefaultActionGroup(connectionClosers)
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val context = DataManager.getInstance.getDataContext(component)
    val title = BspBundle.message("bsp.widget.connections", if (connectionsActive) BspBundle.message("bsp.widget.connections.on") else BspBundle.message("bsp.widget.connections.off"))

    val popup = JBPopupFactory.getInstance.createActionGroupPopup(title, group, context, mnemonics, true)
    val dimension = popup.getContent.getPreferredSize
    val at = new Point(0, -dimension.height)
    popup.show(new RelativePoint(component, at))
  }

  override def updateWidget(): Unit = {
    if (statusBar ne null) {
      statusBar.updateWidget(getId)
    }
  }

  private def connectionsActive: Boolean = {
    val comService = BspCommunicationService.getInstance
    comService.listOpenComms.exists(comService.isAlive.tupled)
  }

  //noinspection ReferencePassedToNls
  private final class CloseBspSession(uri: URI, config: BspServerConfig) extends AnAction(LocalizeValue.of(uri.toString), LocalizeValue.of(BspBundle.message("bsp.widget.kill.bsp.connection.at.uri", uri)), PlatformIconGroup.actionsSuspend()) with DumbAware with AnActionWithSyncUpdate {
    override def update(e: AnActionEvent): Unit = {
      val isAlive = BspCommunicationService.getInstance.isAlive(uri, config)
      e.getPresentation.setEnabled(isAlive)
    }

    override def actionPerformed(e: AnActionEvent): Unit = {
      BspCommunicationService.getInstance.closeCommunication(uri, config)
    }

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private final class CloseAllSessions extends AnAction(LocalizeValue.of(BspBundle.message("bsp.widget.stop.all.bsp.connections")), LocalizeValue.of(BspBundle.message("bsp.widget.stop.all.bsp.connections")), PlatformIconGroup.actionsSuspend()) with DumbAware with AnActionWithSyncUpdate {

    override def update(e: AnActionEvent): Unit = {
      e.getPresentation.setEnabled(connectionsActive)
    }

    override def actionPerformed(e: AnActionEvent): Unit = {
      BspCommunicationService.getInstance.closeAll
    }

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  private final class TerminateServer(uri: URI, config: BspServerConfig) extends AnAction(LocalizeValue.of(BspBundle.message("bsp.widget.bsp.terminate.server", uri)), LocalizeValue.of(BspBundle.message("bsp.widget.bsp.terminate.server", uri)), PlatformIconGroup.actionsExit()) with DumbAware with AnActionWithSyncUpdate {
    override def update(e: AnActionEvent): Unit = {
      val exitCommandDefined = BspCommunicationService.getInstance.exitCommands(uri, config).toOption.exists(_.nonEmpty)
      e.getPresentation.setVisible(exitCommandDefined)
    }

    override def actionPerformed(e: AnActionEvent): Unit = try {
      val commands = BspCommunicationService.getInstance.exitCommands(uri, config).getOrElse(List())
      BspCommunicationService.getInstance.closeAll
      ApplicationManager.getApplication.executeOnPooledThread(
        new Runnable {
          override def run(): Unit = {
            if (commands.isEmpty) {
              BspServerWidgetFactory.logger.info(s"BSP server exit command not set in '${BspCommunication.argvExit}' field")
            } else {
              BspServerWidgetFactory.logger.info(s"Running BSP server restart")
            }
            val timeoutLength = 30
            commands.foreach { command =>
              BspServerWidgetFactory.logger.info(s"Running comand: '${command.mkString(" ")}'")
              val process = new ProcessBuilder(command.asJava).start()
              val timedOut = !process.waitFor(timeoutLength, TimeUnit.SECONDS)
              if (timedOut) {
                // We log only timeouts, and skip regular errors because they occur too often,
                // ex. when the server is just not running.
                val title = s"Timeout when terminating BSP server after ${timeoutLength} seconds"
                val stderr = s"[STDERR]\n${new String(process.getErrorStream.readAllBytes(), StandardCharsets.UTF_8)}\n"
                val stdout = s"[STDOUT]\n${new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)}\n"
                BspServerWidgetFactory.logger.error(s"$title\n $stderr\n $stdout")
              }
            }
          }
        }
      )
    } catch {
      case e: Throwable => BspServerWidgetFactory.logger.error(e)
    }

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }
}
