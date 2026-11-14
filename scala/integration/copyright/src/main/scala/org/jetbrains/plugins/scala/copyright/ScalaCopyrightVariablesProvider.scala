package org.jetbrains.plugins.scala.copyright

import consulo.annotation.component.ExtensionImpl
import consulo.language.copyright.pattern.{CopyrightVariablesProvider, FileInfo}
import consulo.language.psi.PsiFile
import consulo.module.Module
import consulo.project.Project
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

import java.util

@ExtensionImpl
final class ScalaCopyrightVariablesProvider extends CopyrightVariablesProvider {

  import ScalaCopyrightVariablesProvider.findClass

  override def getFileType: FileType = ScalaFileType.INSTANCE

  override def collectVariables(
    context: util.Map[String, AnyRef],
    project: Project,
    module: Module,
    file: PsiFile
  ): Unit = file match {
    case file: ScalaFile =>
      val fileInfo: FileInfo = new FileInfo(file) {
        override def getClassName: String = findClass(file).fold(super.getClassName)(_.name)

        override def getQualifiedClassName: String = findClass(file).fold(super.getQualifiedClassName)(_.qualifiedName)
      }
      context.put("file", fileInfo)
    case _ =>
  }
}

private object ScalaCopyrightVariablesProvider {
  private def findClass(file: ScalaFile): Option[ScTypeDefinition] = file.typeDefinitions.headOption
}
