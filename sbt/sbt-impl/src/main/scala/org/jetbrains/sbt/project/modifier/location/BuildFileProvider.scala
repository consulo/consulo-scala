package org.jetbrains.sbt.project.modifier.location

import com.intellij.openapi.module.Module as IJModule
import consulo.virtualFileSystem.util.{VirtualFileUtil, VirtualFileUtil}
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.language.file.light.LightVirtualFile
import com.intellij.util.LocalTimeCounter
import org.jetbrains.sbt.project.modifier.BuildFileElementType

import java.nio.file.Path
import scala.collection.mutable

trait BuildFileProvider {
  def findBuildFile(module: IJModule, elementType: BuildFileElementType,
                    vfsFileToCopy: mutable.Map[VirtualFile, LightVirtualFile]): Option[BuildFileEntry[PsiFile]] = {

    def findVirtualFile(file: Path) = Option(VfsUtil.findFile(file, true))

    def toLightVirtualFile(origFile: VirtualFile) = vfsFileToCopy.getOrElseUpdate(origFile,
      new LightVirtualFile(origFile, VfsUtilCore.loadText(origFile), LocalTimeCounter.currentTime))

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
