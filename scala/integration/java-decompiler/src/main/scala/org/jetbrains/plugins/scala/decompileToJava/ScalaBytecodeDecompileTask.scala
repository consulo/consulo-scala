package org.jetbrains.plugins.scala.decompileToJava

import consulo.language.editor.FileHighlightingSetting
import consulo.language.editor.highlight.HighlightLevelUtil
import consulo.application.ApplicationManager
import consulo.navigation.OpenFileDescriptorFactory
import consulo.application.progress.{ProgressIndicator, ProgressManager, Task}
import consulo.project.Project
import consulo.localize.LocalizeValue
import consulo.ui.Alerts
import consulo.util.dataholder.Key
import consulo.util.io.FileUtil
import com.intellij.java.language.impl.JavaFileType
import consulo.language.file.light.LightVirtualFile
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiManager
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
            Alerts.okError(LocalizeValue.of(ScalaJavaDecompilerBundle.message("cannot.decompile.filename.colon.message", file.getName, exception.getMessage)))
              .title(LocalizeValue.of(ScalaJavaDecompilerBundle.message("decompiler.error")))
              .showAsync(file.getProject)
          case Success(text) =>
            createDummyDecompiledFileWithText(file.getProject, text, file.getName)
        }
      }
    }
  }
}

object ScalaBytecodeDecompileTask {
  private val DecompiledKey: Key[java.lang.Boolean] = Key.create("scala.decompiled.java.file")

  def isDecompiled(file: VirtualFile): Boolean =
    java.lang.Boolean.TRUE == file.getUserData(DecompiledKey)

  def showDecompiledJavaCode(file: ScFile): Unit =
    ProgressManager.getInstance().run(new ScalaBytecodeDecompileTask(file))

  // upstream: the file is created in the "scala-decompiled" root of DummyFileSystem
  private def createDummyDecompiledFileWithText(
    project: Project,
    text: String,
    originalFileName: String,
  ): Unit = {
    val decompiledFileName = FileUtil.getNameWithoutExtension(originalFileName) + ".decompiled.java"
    val dummyFile = new LightVirtualFile(decompiledFileName, JavaFileType.INSTANCE, text)
    dummyFile.setWritable(false)
    dummyFile.putUserData(DecompiledKey, java.lang.Boolean.TRUE)

    val psiFile = PsiManager.getInstance(project).findFile(dummyFile)
    if (psiFile != null) {
      HighlightLevelUtil.forceRootHighlighting(psiFile, FileHighlightingSetting.SKIP_HIGHLIGHTING)
    }

    OpenFileDescriptorFactory.getInstance(project).builder(dummyFile).build().navigate(true)
  }
}
