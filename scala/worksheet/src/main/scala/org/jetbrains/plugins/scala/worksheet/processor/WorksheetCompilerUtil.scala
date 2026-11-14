package org.jetbrains.plugins.scala.worksheet.processor

import consulo.codeEditor.LogicalPosition
import consulo.compiler.{CompilerMessage, CompilerMessageCategory}
import consulo.project.Project
import consulo.project.ui.view.MessageView
import consulo.ui.ex.MessageCategory
import consulo.ui.ex.content.ContentFactory
import consulo.ui.ex.errorTreeView.{NewErrorTreeViewPanel, NewErrorTreeViewPanelFactory}
import consulo.virtualFileSystem.{VirtualFile, VirtualFileManager}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.worksheet.ui.printers.repl.QueuedPsi

import scala.util.Try


private[worksheet]
object WorksheetCompilerUtil {
  private val ERROR_CONTENT_NAME = "Worksheet errors" // TODO: is it effectively used?

  sealed trait WorksheetCompileRunRequest
  object WorksheetCompileRunRequest {
    final case class RunRepl(code: String, evaluatedElements: Seq[QueuedPsi]) extends WorksheetCompileRunRequest
    final case class RunCompile(code: String, className: String) extends WorksheetCompileRunRequest
  }
  
  sealed trait CompilationMessageSeverity {
    def toType: Int
    def isFatal: Boolean = false
  }

  object CompilationMessageSeverity {

    def apply(value: String): Option[CompilationMessageSeverity] = value match {
      case "INFO"    => Some(InfoSeverity)
      case "WARNING" => Some(WarningSeverity)
      case "ERROR"   => Some(ErrorSeverity)
      case _         => None
    }
  }

  object ErrorSeverity extends CompilationMessageSeverity {
    override def toType: Int = MessageCategory.ERROR
    override def isFatal = true
  }

  object WarningSeverity extends CompilationMessageSeverity {
    override def toType: Int = MessageCategory.WARNING
  }

  object InfoSeverity extends CompilationMessageSeverity {
    override def toType: Int = MessageCategory.INFORMATION
  }

  def showCompilationMessage(file: VirtualFile, pos: LogicalPosition,
                             msg: Array[String], severity: CompilationMessageSeverity,
                             onShow: () => Unit)
                            (implicit project: Project): Unit = {
    invokeLater {
      if (file != null && file.isValid) {
        addMessagesToView(project, Seq((severity.toType, msg, file, pos.line, pos.column)))
        onShow()
      }
    }
  }

  /**
   * upstream: the compiler messages of a worksheet evaluation are shown by CompilerTask in the build tool window
   * (CompilerTask is internal in Consulo)
   */
  def showCompilerMessages(project: Project, messages: Seq[CompilerMessage]): Unit =
    if (messages.nonEmpty) invokeLater {
      if (!project.isDisposed) {
        val fileManager = VirtualFileManager.getInstance
        val viewMessages = messages.map { message =>
          val category = message.getCategory match {
            case CompilerMessageCategory.ERROR   => MessageCategory.ERROR
            case CompilerMessageCategory.WARNING => MessageCategory.WARNING
            case _                               => MessageCategory.INFORMATION
          }
          val file = Option(message.getUrl).map(fileManager.findFileByUrl).orNull
          // error tree view lines and columns are 0-based
          (category, message.getMessage.get.split("\n"), file, message.getLine - 1, message.getColumn - 1)
        }
        addMessagesToView(project, viewMessages)
      }
    }

  private def addMessagesToView(project: Project, messages: Seq[(Int, Array[String], VirtualFile, Int, Int)]): Unit = {
    val messageView = MessageView.getInstance(project)
    messageView.runWhenInitialized { () =>
      val contentManager = messageView.getContentManager
      val (currentContent, treeView) =
        Option(contentManager.findContent(ERROR_CONTENT_NAME)) match {
          case Some(old) if old.getUserData(ErrorTreeViewKey) != null =>
            (old, old.getUserData(ErrorTreeViewKey))
          case _ =>
            val newView = consulo.application.Application.get.getInstance(classOf[NewErrorTreeViewPanelFactory]).createPanel(project, null, false, true, null)
            //noinspection ReferencePassedToNls
            val errorContent = ContentFactory.getInstance.createContent(newView.getComponent, ERROR_CONTENT_NAME, true)
            errorContent.putUserData(ErrorTreeViewKey, newView)
            errorContent.setDisposer(newView)
            contentManager.addContent(errorContent)
            (errorContent, newView)
        }
      messages.foreach { case (category, text, file, line, column) =>
        treeView.addMessage(category, text, file, line, column, null)
      }

      contentManager.setSelectedContent(currentContent)
      messageView.getToolWindow.activate(null, false)
    }
  }

  private val ErrorTreeViewKey = consulo.util.dataholder.Key.create[NewErrorTreeViewPanel]("WORKSHEET_ERROR_TREE_VIEW")

  def showCompilationError(file: VirtualFile, pos: LogicalPosition, msg: Array[String], onShow: () => Unit)
                          (implicit project: Project): Unit =
    showCompilationMessage(file, pos, msg, ErrorSeverity, onShow)

  // MessageViewImpl has been rewritten to Kotlin coroutines and is lazily, asynchronously loaded. If the MessageView
  // instance for the project has not been fully initialized, `getInstance` throws an IllegalStateException.
  // For the purposes of not complicating the code, we catch the exception and ignore it. If the MessageView instance
  // has not been initialized, there is no old content to remove.
  def removeOldMessageContent(project: Project): Unit = invokeLater {
    if (!project.isDisposed) Try {
      val contentManager = MessageView.getInstance(project).getContentManager
      val oldContent = contentManager.findContent(ERROR_CONTENT_NAME)
      if (oldContent != null) {
        contentManager.removeContent(oldContent, true)
      }
    }
  }
}
