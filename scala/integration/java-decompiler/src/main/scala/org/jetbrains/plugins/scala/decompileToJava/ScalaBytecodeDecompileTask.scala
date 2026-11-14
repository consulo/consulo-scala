package org.jetbrains.plugins.scala.decompileToJava

import consulo.language.editor.FileHighlightingSetting
import consulo.language.editor.highlight.HighlightLevelUtil
import consulo.application.ApplicationManager
import consulo.document.FileDocumentManager
import consulo.navigation.OpenFileDescriptor
import consulo.application.progress.{ProgressIndicator, ProgressManager, Task}
import consulo.project.Project
import consulo.ui.ex.awt.Messages
import consulo.util.io.FileUtil
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import consulo.virtualFileSystem.{VirtualFile, VirtualFileManager}
import consulo.language.psi.PsiManager
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

import scala.util.{Failure, Success}

class ScalaBytecodeDecompileTask(file: ScFile)
    extends Task.Backgroundable(file.getProject, ScalaJavaDecompilerBundle.message("decompile.scala.bytecode")) {

  import ScalaBytecodeDecompileTask._

  override def run(indicator: ProgressIndicator): Unit = {
    indicator.setText(ScalaJavaDecompilerBundle.message("decompiling.file", file.getName))

    val tryDecompile = ScalaDecompilerService().decompile(file)

    ApplicationManager.getApplication.invokeLater { () =>
      if (file.isValid && !file.getProject.isDisposed) {
        tryDecompile match {
          case Failure(exception) =>
            Messages.showErrorDialog(
              ScalaJavaDecompilerBundle.message("cannot.decompile.filename.colon.message", file.getName, exception.getMessage),
              ScalaJavaDecompilerBundle.message("decompiler.error")
            )
          case Success(text) =>
            inWriteAction {
              createDummyDecompiledFileWithText(myProject, text, file.getName)
            }
        }
      }
    }
  }
}

object ScalaBytecodeDecompileTask {
  private[decompileToJava] val scalaDecompiledFolder = "scala-decompiled"
  private[decompileToJava] val scalaDecompiledRoot   = s"dummy://$scalaDecompiledFolder"

  def showDecompiledJavaCode(file: ScFile): Unit =
    ProgressManager.getInstance().run(new ScalaBytecodeDecompileTask(file))

  private def getOrCreateDummyRoot(): VirtualFile = {
    val root = VirtualFileManager.getInstance().refreshAndFindFileByUrl(scalaDecompiledRoot)
    if (root == null)
      DummyFileSystem.getInstance().createRoot(scalaDecompiledFolder)
    else
      root
  }

  private def createDummyDecompiledFileWithText(
    project: Project,
    text: String,
    originalFileName: String,
  ): Unit = {
    val dummyRoot = getOrCreateDummyRoot()
    val decompiledFileName = FileUtil.getNameWithoutExtension(originalFileName) + ".decompiled.java"
    val dummyFile = DummyFileSystem.getInstance().createChildFile(null, dummyRoot, decompiledFileName)

    val psiFile = PsiManager.getInstance(project).findFile(dummyFile)
    if (psiFile != null) {
      HighlightLevelUtil.forceRootHighlighting(psiFile, FileHighlightingSetting.SKIP_HIGHLIGHTING)
    }
    val document = FileDocumentManager.getInstance().getDocument(dummyFile)
    if (document != null) {
      document.setText(text)
    }
    else {
      throw new AssertionError(s"Can't find document of dummy decompiled file, original file name: $originalFileName")
    }

    new OpenFileDescriptor(project, dummyFile).navigate(true)
  }
}
