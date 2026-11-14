package org.jetbrains.plugins.scala.project.template

import consulo.application.CommonBundle
import consulo.logging.Logger
import consulo.application.progress.{ProgressIndicator, Task}
import consulo.ui.ex.awt.{DialogWrapper, Messages}
import consulo.virtualFileSystem.VirtualFile
import consulo.ui.ex.awt.JBScrollPane
import consulo.ui.ex.awt.table.TableView
import org.jetbrains.plugins.scala.project.sdkdetect.ScalaSdkProvider
import org.jetbrains.plugins.scala.project.sdkdetect.repository.ScalaSdkDetector
import org.jetbrains.plugins.scala.project.template.ScalaVersionDownloadingDialog.ScalaVersionResolveResult
import org.jetbrains.plugins.scala.project.template.SdkSelectionDialogWrapper.{showDuplicatedFilesError, validateSdk}
import org.jetbrains.plugins.scala.project.template.sdk_browse.ExplicitSdkSelection
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.ScalaBundle

import java.awt.{BorderLayout, Dimension}
import java.awt.event.ActionEvent
import java.nio.file.Path
import java.util.Collections
import javax.swing._
import javax.swing.event.ListSelectionEvent
import scala.jdk.CollectionConverters.ListHasAsScala

class SdkSelectionDialogWrapper(contextDirectory: VirtualFile) extends DialogWrapper(true) {

  import org.jetbrains.plugins.scala.project.template.SdkSelectionDialogWrapper.SdkValidationError._

  private val myTable = new TableView[SdkChoice]
  private val myTableModel = new SdkTableModel

  private var sdkScanIndicator: Option[ProgressIndicator] = None

  private var mySelectedSdk: Option[ScalaSdkDescriptor] = None

  locally {
    setTitle(ScalaBundle.message("sdk.create.select.files"))
    setModal(true)
    init()

    runSdkScanTask()
  }

  override def createCenterPanel(): JComponent = {
    val panel = new JPanel(new BorderLayout)

    myTable.setModelAndUpdateColumns(myTableModel)
    myTable.getSelectionModel.addListSelectionListener((_: ListSelectionEvent) => updateOkButtonEnabled())
    myTable.getColumnModel.getSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    updateOkButtonEnabled()

    val scrollPane = new JBScrollPane
    scrollPane.setViewportView(myTable)

    scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize.width, 500))
    panel.add(scrollPane, BorderLayout.CENTER)
    panel
  }

  private def updateOkButtonEnabled(): Unit = {
    val isSdkSelected = myTable.getSelectedRow >= 0
    getOKAction.setEnabled(isSdkSelected)
  }

  override def doCancelAction(): Unit = {
    super.doCancelAction()
    onCancel()
  }

  override def doOKAction(): Unit = {
    val sdk = selectedSdkFromTable
    sdk match {
      case Some(sdk) =>
        validateSdk(sdk) match {
          case Left(duplicatedFiles: DuplicatedFiles) =>
            showDuplicatedFilesError(this.getContentPanel, duplicatedFiles)
            return
          case Right(_)                               =>
        }
      case _         =>
    }
    mySelectedSdk = selectedSdkFromTable

    super.doOKAction()
    closeDialogGracefully()
  }

  private def selectedSdkFromTable: Option[ScalaSdkDescriptor] = {
    if (myTable.getSelectedRowCount > 0)
      Some(myTableModel.getItems.get(myTable.getSelectedRow).sdk)
    else None
  }

  override def createLeftSideActions(): Array[Action] = {
    val downloadAction = new DialogWrapperAction(ScalaBundle.message("scala.sdk.selection.button.download")) {
      override def doAction(actionEvent: ActionEvent): Unit = onDownload()
    }
    val browseAction = new DialogWrapperAction(ScalaBundle.message("scala.sdk.selection.button.browse")) {
      override def doAction(actionEvent: ActionEvent): Unit = onBrowse()
    }
    Array(downloadAction, browseAction)
  }

  private val ScalaLibraryFileNames = Artifact.ScalaLibraryAndModulesArtifacts.map(_.prefix)

  private def onDownload(): Unit = {
    val resolvedScalaVersion = new ScalaVersionDownloadingDialog(this.getContentPanel).showAndGetSelected()
    resolvedScalaVersion.foreach { case ScalaVersionResolveResult(version, compilerJars, librarySourcesJars, compilerBridgeJar, replClasspath) =>
      val libraryJars = compilerJars.filter(f => ScalaLibraryFileNames.exists(f.getFileName.toString.startsWith(_)))
      val scaladocExtraClasspath = Nil // TODO SCL-17219
      val sdkDescriptor = ScalaSdkDescriptor(Some(version), None, compilerJars, scaladocExtraClasspath, libraryJars, librarySourcesJars, Nil /*docs are not downloaded*/, compilerBridgeJar, Some(replClasspath))
      closeDialogGracefully(Some(sdkDescriptor))
    }
  }

  private def onBrowse(): Unit = {
    ExplicitSdkSelection.chooseScalaSdkFiles(myTable).thenAccept { resultOpt =>
      resultOpt.foreach { sdk =>
        closeDialogGracefully(Some(sdk))
      }
    }
  }

  private def onCancel(): Unit = {
    closeDialogGracefully(None)
  }

  def open(): Option[ScalaSdkDescriptor] = {
    pack()
    show()
    mySelectedSdk
  }

  private def closeDialogGracefully(sdkDescriptor: Option[ScalaSdkDescriptor]): Unit = {
    mySelectedSdk = sdkDescriptor
    closeDialogGracefully()
  }

  private def closeDialogGracefully(): Unit = {
    cancelCurrentSdkScanning()
    dispose()
  }

  private def runSdkScanTask(): Unit = {
    val scanTask = new SdkScanTask()
    scanTask.queue()
  }

  private def cancelCurrentSdkScanning(): Unit = {
    sdkScanIndicator.foreach(_.cancel())
    sdkScanIndicator = None
  }

  private class SdkScanTask
    extends Task.Backgroundable(null, ScalaBundle.message("sdk.scan.title", ""), true) {

    override def run(indicator: ProgressIndicator): Unit = {
      sdkScanIndicator = Some(indicator)

      val scalaJarDetectors = ScalaSdkDetector.allDetectors(contextDirectory)
      val scalaSdkProvider = new ScalaSdkProvider(indicator, scalaJarDetectors)
      scalaSdkProvider.discoverSDKs(this.addToTable, SwingUtilities.invokeLater { () =>
        if (myTable.getSelectedRow == -1) {
          myTable.getItems.asScala.find(!_.sdk.isScala3).foreach(sdk => myTable.setSelection(Collections.singleton(sdk)))
        }
      })

      sdkScanIndicator = None
    }

    private def addToTable(sdkChoice: SdkChoice): Unit = {
      // NOTE: it should be `SwingUtilities.invokeLater`, not `ApplicationManager.getApplication.invokeLater`
      //  otherwise requests will be postponed until dialog is closed (dialogWrapper.show causes it)
      SwingUtilities.invokeLater(() => {
        val previousSelection = myTable.getSelectedRow
        myTableModel.addRow(sdkChoice)
        myTableModel.fireTableDataChanged()
        if (previousSelection >= 0) {
          myTable.getSelectionModel.setSelectionInterval(previousSelection, previousSelection)
        }
      })
    }
  }
}

object SdkSelectionDialogWrapper {

  private val Log = Logger.getInstance(classOf[SdkSelectionDialogWrapper])

  private sealed trait SdkValidationError

  private object SdkValidationError {
    final case class DuplicatedFiles(duplicates: Map[String, Seq[Path]], componentName: NlsString) extends SdkValidationError
  }

  import SdkValidationError._

  private def validateSdk(descriptor: ScalaSdkDescriptor): Either[SdkValidationError, Unit] = {
    for {
      _ <- assertNoDuplicates(descriptor.compilerClasspath, NlsString(ScalaBundle.message("scala.sdk.component.name.compiler.classpath")))
      _ <- assertNoDuplicates(descriptor.libraryFiles, NlsString(ScalaBundle.message("scala.sdk.component.name.library")))
      _ <- assertNoDuplicates(descriptor.sourceFiles, NlsString(ScalaBundle.message("scala.sdk.component.name.library.source")))
      _ <- assertNoDuplicates(descriptor.docFiles, NlsString(ScalaBundle.message("scala.sdk.component.name.library.scaladoc")))
    } yield ()
  }

  /**
   * @return Left(duplicates) if there are some files with the same name (but potentially with different paths, for any reason)
   */
  private def assertNoDuplicates(files: Seq[Path], componentName: NlsString): Either[DuplicatedFiles, Unit] = {
    val nameToFiles = files.groupBy(_.getFileName.toString)
    val duplicates = nameToFiles.filter(_._2.lengthCompare(1) > 0)
    if (duplicates.isEmpty)
      Right(())
    else
      Left(DuplicatedFiles(duplicates, componentName))
  }

  private def showDuplicatedFilesError(component: JComponent, duplicatedFiles: DuplicatedFiles): Unit = {
    Log.warn(s"Duplicate files found: $duplicatedFiles") // warn (no error) just not to show the error in EA (we already show the dialog)

    val DuplicatedFiles(duplicates, componentName) = duplicatedFiles

    val messageLine1 = ScalaBundle.message("scala.sdk.descriptor.contains.duplicated.files", componentName)
    val messageLineOther = duplicates
      .map { case (name, files) => s"$name: ${files.mkString(",")}" }
      .mkString("\n")
    val message = messageLine1 + "\n" + messageLineOther

    val title = CommonBundle.message("title.error")
    Messages.showErrorDialog(component, message, title)
  }
}
