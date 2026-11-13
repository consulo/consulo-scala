package org.jetbrains.plugins.scala.lang.psi.compiled

import consulo.application.Application
import consulo.language.Language
import consulo.virtualFileSystem.BinaryFileDecompiler
import consulo.virtualFileSystem.fileType.FileType
import consulo.virtualFileSystem.VirtualFile
import com.intellij.java.language.impl.psi.impl.compiled.ClassFileStubBuilder
import consulo.language.file.{FileViewProvider, FileViewProviderFactory}
import consulo.language.psi.PsiManager
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import consulo.ui.image.Image

/**
 * Scala signatures which are usually stored in .class files, as a separate files
 * https://github.com/scala/scala/pull/7712
 * */
object SigFileType extends FileType {
  @NonNls
  override def getId = "SIG"

  override def getDescription: LocalizeValue = LocalizeValue.of(ScalaBundle.message("file.type.scala.outlines"))

  override def getDefaultExtension = "sig"

  override def getIcon: Image = Image.empty(Image.DEFAULT_ICON_SIZE)

  override def isBinary = true

  override def isReadOnly = true
}


class SigFileViewProviderFactory extends FileViewProviderFactory {
  override def createFileViewProvider(file: VirtualFile,
                                      language: Language,
                                      manager: PsiManager,
                                      eventSystemEnabled: Boolean): FileViewProvider = {

    ScClassFileDecompiler.createFileViewProviderImpl(manager, file, eventSystemEnabled, ScalaLanguage.INSTANCE)
  }
}

class SigFileStubBuilder extends ClassFileStubBuilder(Application.get()) {
  override def getFileType: FileType = SigFileType
}

class SigFileDecompiler extends BinaryFileDecompiler {
  override def decompile(file: VirtualFile): CharSequence = {
    DecompilationResult.sourceNameAndText(file).map(_._2).getOrElse(ScalaBundle.message("could.not.decompile.file.comment", file.getName))
  }
}