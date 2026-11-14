package org.jetbrains.plugins.scala.compiler.highlighting.services.requests

import consulo.document.Document
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.compilers.WorksheetHighlightingCompiler
import org.jetbrains.plugins.scala.compiler.highlighting.services.util.CompilationUtils
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.project.ProjectPsiFileExt

/**
 * upstream: an incremental compilation of the worksheet's module runs first when the worksheet is highlighted for the
 * first time, here the worksheet is compiled against the module output as it is (use Build to update it)
 */
class WorksheetRequest(
  val file: ScalaFile,
  val virtualFile: VirtualFile,
  val document: Document,
  protected val project: Project
) extends CompilationRequest {

  private val Log = Logger.getInstance(classOf[WorksheetRequest])

  override def originFiles: Map[VirtualFile, Document] = Map(virtualFile -> document)

  override private[services] def execute(): Unit = {
    //Note, we don't need to invoke `findRepresentativeModuleForSharedSourceModuleOrSelf`
    //because it's already called for all worksheets in WorksheetSyntheticModuleService
    val module = file.module match {
      case Some(m) => m
      case None =>
        Log.warn(s"[${project.getName}] can't find module for worksheet ${file.getName}")
        return
    }

    CompilationUtils.performCompilation(project, Set(virtualFile), reportedDocumentVersions) { client =>
      WorksheetHighlightingCompiler.compile(file, document, module, client)
    }
  }
}
