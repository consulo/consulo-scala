package org.jetbrains.plugins.scala.api
package lang.psi.api

import com.intellij.java.language.psi.PsiClassOwnerEx
import consulo.language.file.light.LightVirtualFile
import consulo.language.psi.stub.PsiFileWithStubSupport
import consulo.virtualFileSystem.VirtualFile

trait ScFile extends PsiFileWithStubSupport
  with PsiClassOwnerEx {

  def isCompiled: Boolean
}

object ScFile {

  implicit class ScFileExt(private val file: ScFile) extends AnyVal {

    def findVirtualFile: Option[VirtualFile] = VirtualFile.unapply(file)
  }

  object VirtualFile {

    def unapply(file: ScFile): Option[VirtualFile] = {
      val originalFile = file.getOriginalFile
      originalFile.getVirtualFile match {
        case null =>
          file.getViewProvider.getVirtualFile match {
            case vFile: LightVirtualFile =>
              //vFile.getOriginalFile can return null for example when the file is created via:
              //`com.intellij.psi.PsiFileFactory.createFileFromText(java.lang.String, java.lang.String)`
              // or `com.intellij.psi.PsiFileFactory.createFileFromText(java.lang.String, com.intellij.openapi.fileTypes.FileType, java.lang.CharSequence)`
              //which can be used in tests
              Option(vFile.getOriginalFile).orElse(Some(vFile))
            case _ => None
          }
        case virtualFile =>
          Some(virtualFile)
      }
    }
  }

}
