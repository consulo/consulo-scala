package org.jetbrains.sbt
package project


import consulo.fileChooser.FileChooserDescriptor
import org.jetbrains.sbt.api.language.SbtFileType
import consulo.virtualFileSystem.VirtualFile

// upstream: extends OpenProjectFileChooserDescriptor (internal in Consulo)
class SbtOpenProjectDescriptor extends FileChooserDescriptor(true, true, false, false, false, false) {

  override def isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean =
    super.isFileVisible(file, showHiddenFiles) &&
      (file.isDirectory || SbtFileType.isMyFileType(file))

  override def isFileSelectable(file: VirtualFile): Boolean =
    super.isFileSelectable(file) &&
      SbtProjectImportProvider.canImport(file)
}
