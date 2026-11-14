package org.jetbrains.sbt.project.modifier.location

import consulo.module.Module as IJModule
import consulo.virtualFileSystem.util.VirtualFileUtil
import consulo.virtualFileSystem.{LocalFileSystem, VirtualFile}
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.language.file.light.LightVirtualFile
import consulo.util.lang.LocalTimeCounter
import org.jetbrains.sbt.project.modifier.BuildFileElementType

import java.nio.file.Path
import scala.collection.mutable

trait BuildFileProvider {
  def findBuildFile(module: IJModule, elementType: BuildFileElementType,
                    vfsFileToCopy: mutable.Map[VirtualFile, LightVirtualFile]): Option[BuildFileEntry[PsiFile]] = {

    def findVirtualFile(file: Path) = Option(LocalFileSystem.getInstance.refreshAndFindFileByNioFile(file))

    def toLightVirtualFile(origFile: VirtualFile) = vfsFileToCopy.getOrElseUpdate(origFile,
      new LightVirtualFile(origFile, VirtualFileUtil.loadText(origFile), LocalTimeCounter.currentTime))

    def toPsiFile(vFile: VirtualFile) = Option(PsiManager.getInstance(module.getProject).findFile(vFile))

    findIoFile(module, elementType).flatMap {
      case BuildFileEntry(buildFile, isModuleLocal) =>
        findVirtualFile(buildFile)
          .map(toLightVirtualFile)
          .flatMap(toPsiFile)
          .map(BuildFileEntry(_, isModuleLocal))
    }
  }


  def findIoFile(module: IJModule, elementType: BuildFileElementType): Option[BuildFileEntry[Path]]
}
