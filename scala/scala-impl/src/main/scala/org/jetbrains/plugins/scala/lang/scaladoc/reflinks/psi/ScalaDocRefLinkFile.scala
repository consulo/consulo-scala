package org.jetbrains.plugins.scala.lang.scaladoc.reflinks.psi

import consulo.language.impl.psi.PsiFileBase
import consulo.virtualFileSystem.fileType.FileType
import consulo.language.file.FileViewProvider
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.lang.scaladoc.reflinks.ScalaDocRefLinkLanguage

class ScalaDocRefLinkFile(viewProvider: FileViewProvider)
  extends PsiFileBase(viewProvider, ScalaDocRefLinkLanguage.INSTANCE) {

  override def getFileType: FileType = ScalaFileType.INSTANCE

  override def toString: String = "ScalaDocRefLinkFile"
}
