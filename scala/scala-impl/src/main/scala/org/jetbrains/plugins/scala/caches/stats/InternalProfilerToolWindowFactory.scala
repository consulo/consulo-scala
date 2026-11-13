package org.jetbrains.plugins.scala.caches.stats

import consulo.annotation.component.ExtensionImpl

import consulo.ui.image.Image
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.application.AllIcons
import consulo.project.ui.notification.{NotificationGroup, NotificationService, NotificationType}
import consulo.localize.LocalizeValue
import consulo.ui.ex.action.{ActionManager, ActionUpdateThread, AnAction, AnActionEvent, DefaultActionGroup}
import consulo.application.ApplicationManager
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.ui.ex.toolWindow.ToolWindow
import consulo.project.ui.wm.ToolWindowFactory
import consulo.ui.ex.awt.JBScrollPane
import consulo.ui.ex.content.{Content, ContentFactory}
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.plugins.scala.util.ui.extensions.ComponentExt

import java.awt.BorderLayout
import java.util.concurrent.{Future, TimeUnit}
import javax.swing.JPanel
import scala.jdk.CollectionConverters._

@ExtensionImpl(id = "internal-profiler")
class InternalProfilerToolWindowFactory extends ToolWindowFactory with DumbAware {

  //noinspection UnstableApiUsage,ApiStatus
  override def getIcon: Image = AllIcons.Toolwindows.ToolWindowRun // upstream: AllIcons.Toolwindows.ToolWindowProfiler

  override def init(toolWindow: ToolWindow): Unit = {
    toolWindow.setStripeTitle(NlsString.force("Scala plugin profiler"))
  }

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    InternalProfilerToolWindowFactory.createContent(project).foreach {
      toolWindow.getContentManager.addContent
    }
  }

  override def shouldBeAvailable(project: Project): Boolean =
    ApplicationManager.getApplication.isInternal
}

//noinspection ScalaExtractStringToBundle
object InternalProfilerToolWindowFactory {
  val ID = "internal-profiler"

  private def NotificationGroup: NotificationGroup = ScalaNotificationGroups.scalaGeneral

  lazy val timingsModel: DataByIdTableModel[TracerData] = {
    val dataById = new DataById[TracerData](_.id)
    new DataByIdTableModel(dataById,
      dataById.stringColumn("Computation", _.name),
      dataById.numColumn("Invoked", _.totalCount),
      dataById.numColumn("Read from cache", _.fromCacheCount),
      dataById.numColumn("Actually computed", _.actualCount),
      dataById.numColumn("Max Time, ms", _.maxTime),
      dataById.numColumn("Total Time, ms", _.totalTime),
      dataById.numColumn("Own Time, ms", _.ownTime),
      dataById.numColumn("Avg Time, ms", _.avgTime)
    )(preferredWidths = Seq(5, 1, 1, 1, 1, 1, 1, 1))
  }

  lazy val parentCallsModel: DataByIdTableModel[TracerData] = {
    val dataById = new DataById[TracerData](_.id)
    new DataByIdTableModel(dataById,
      dataById.stringColumn("Computation", _.name),
      dataById.numColumn("Actually computed", _.actualCount),
      dataById.numColumn("Total Time, ms", _.totalTime),
      dataById.stringColumn("Parent calls", parentCallsText)
    )(preferredWidths = Seq(6, 1, 1, 12))
  }

  lazy val memoryModel: DataByIdTableModel[MemoryData] = {
    val dataById = new DataById[MemoryData](_.id)
    new DataByIdTableModel(
      dataById,
      dataById.stringColumn("Computation", _.name),
      dataById.numColumn("Tracked caches", _.trackedCaches),
      dataById.numColumn("Tracked cache entries", _.trackedCacheEntries)
    )(preferredWidths = Seq(5, 1, 1))
  }

  lazy val modificationTrackersModel: DataByIdTableModel[ModificationTrackersData] = {
    val dataById = new DataById[ModificationTrackersData](_.uniqueName)
    new DataByIdTableModel(
      dataById,
      dataById.stringColumn("ModificationTracker", _.uniqueName),
      dataById.numColumn("Modification Count", _.modificationCount),
    )(preferredWidths = Seq(1, 5))
  }

  private def parentCallsText(data: TracerData): String = {
    val parentCalls = data.parentCalls
    val sorted = parentCalls.asScala.sortBy(_._2).reverse
    val total = sorted.map(_._2).sum

    def fraction(i: Int): String = (i.toDouble / total * 100).round.toString + " %"

    sorted.map {
      case (name, count) => s"$name: ${fraction(count)}"
    }.mkString("; ")
  }

  def createContent(project: Project): Seq[Content] = {
    val timingsTable = createTableWithToolbarPanel(ScalaCacheTracerDataSource, timingsModel, project)
    val parentCalls = createTableWithToolbarPanel(ScalaCacheTracerDataSource, parentCallsModel, project)
    val memory = createTableWithToolbarPanel(ScalaCacheMemoryDataSource, memoryModel, project)
    val modificationTrackers = createTableWithToolbarPanel(new ScalaModificationTrackersDataSource(project), modificationTrackersModel, project)

    val factory = ContentFactory.getInstance()
    Seq(
      factory.createContent(timingsTable, "Timings", false),
      factory.createContent(parentCalls, "Parent Calls", false),
      factory.createContent(memory, "Memory", false),
      factory.createContent(modificationTrackers, "Modification Trackers", false)
    )
  }

  def createTableWithToolbarPanel[Data](dataSource: DataSource[Data], tableModel: DataByIdTableModel[Data], project: Project): JPanel = {
    val actionToolbarPanel = new JPanel
    val actionGroup = new DefaultActionGroup(
      new RunPauseAction(dataSource),
      new ClearAction(dataSource, tableModel),
      new CacheContentClearAction(project)
    )
    val actionToolBar = ActionManager.getInstance().createActionToolbar(ID, actionGroup, false)
    actionToolbarPanel.setLayout(new BorderLayout)
    actionToolbarPanel.add(actionToolBar.getComponent)

    val table = tableModel.createTable()
    tableModel.registerSpeedSearch(table)

    val scrollPane = new JBScrollPane
    scrollPane.setViewportView(table)

    val mainPanel = new JPanel()
    actionToolBar.setTargetComponent(mainPanel)
    mainPanel.setLayout(new BorderLayout)
    mainPanel.add(actionToolbarPanel, BorderLayout.WEST)
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    mainPanel.bindExecutionToVisibility { () =>
      scheduleRefresh(tableModel, dataSource)
    }

    mainPanel
  }

  def scheduleRefresh[Data](tableModel: DataByIdTableModel[Data], dataSource: DataSource[Data]): Future[?] = {
    val refreshRateMs = 500L

    AppExecutorUtil.getAppScheduledExecutorService
      .scheduleWithFixedDelay(() => {
        ApplicationManager.getApplication.invokeLater(() =>
          tableModel.refresh(dataSource.getCurrentData)
        )
      }, 0, refreshRateMs, TimeUnit.MILLISECONDS)
  }

  class RunPauseAction(dataSource: DataSource[?]) extends AnAction with DumbAware {

    private def currentIcon(): Image = {
      if (dataSource.isActive)
        AllIcons.Actions.Pause
      else AllIcons.Actions.Resume
    }

    override def update(e: AnActionEvent): Unit = {
      e.getPresentation.setIcon(currentIcon())
    }

    override def actionPerformed(e: AnActionEvent): Unit = {
      if (dataSource.isActive) dataSource.stop()
      else dataSource.resume()
    }

    def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
  }

  class ClearAction(dataSource: DataSource[?], tableModel: DataByIdTableModel[?]) extends AnAction with DumbAware {
    getTemplatePresentation.setIcon(AllIcons.Actions.GC)

    override def actionPerformed(e: AnActionEvent): Unit = {
      dataSource.clear()
      tableModel.clear()
    }

    def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  class CacheContentClearAction(project: Project) extends AnAction with DumbAware {
    getTemplatePresentation.setIcon(AllIcons.Actions.Uninstall)
    getTemplatePresentation.setText("Clear Cache Contents")
    // AllIcons.Actions.Uninstall
    // AllIcons.Actions.Unselectall

    override def actionPerformed(e: AnActionEvent): Unit = {
      def toMB(bytes: Double) = bytes / (1024.0 * 1024.0)
      def toPercent(d: Double) = d * 100.0
      val runtime = Runtime.getRuntime
      def usedMemory() = toMB((runtime.totalMemory - runtime.freeMemory).toDouble)

      val memoryBeforePrepare = usedMemory()
      System.gc()
      val cacheEntityCount = CacheTracker.tracked.values.foldLeft(0)(_ + _.cachedEntityCount)
      val memoryBeforeCacheFreeing = usedMemory()
      CacheTracker.clearAllCaches()
      System.gc()
      val memoryAfter = usedMemory()

      val preFreed = memoryBeforePrepare - memoryBeforeCacheFreeing
      val cacheFreed = memoryBeforeCacheFreeing - memoryAfter
      val totalFreed = preFreed + cacheFreed
      val total = toMB(runtime.totalMemory.toDouble)

      NotificationService.getInstance.newOfType(NotificationGroup, NotificationType.INFORMATION)
        .title(LocalizeValue.of(s"Cache cleared ($cacheEntityCount entities)"))
        .content(LocalizeValue.of(        f"""<html>
           |<body>
           |<table>
           |<tr><td>Pre GC: </td><td>$preFreed%1.1f MB</td><td>(${toPercent(preFreed/memoryBeforePrepare)}%1.1f%%)</td></tr>
           |<tr><td>Cache GC: </td><td>$cacheFreed%1.1f MB</td><td>(${toPercent(cacheFreed/memoryBeforePrepare)}%1.1f%%)</td></tr>
           |<tr><td>Total GC: </td><td>$totalFreed%1.1f MB</td><td>(${toPercent(totalFreed / memoryBeforePrepare)}%1.1f%%)</td></tr>
           |<tr><td>Used before: </td><td>$memoryBeforePrepare%1.1f MB</td><td></td></tr>
           |<tr><td>Used after: </td><td>$memoryAfter%1.1f MB</td><td></td></tr>
           |<tr><td>Available: </td><td>$total%1.1f MB</td><td></td></tr>
           |</table>
           |</body>
           |</html>""".stripMargin))
        .notify(project)
    }

    def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }
}
