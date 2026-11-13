package org.jetbrains.plugins.scala.project.template.sdk_browse

import consulo.component.ComponentManager
import consulo.fileChooser.FileChooser
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.project.template.ScalaSdkDescriptor

import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

private[template] object ExplicitSdkSelection {

  // upstream: synchronous FileChooser.chooseFiles returning Option[ScalaSdkDescriptor]
  def chooseScalaSdkFiles(parentComponent: JComponent): CompletableFuture[Option[ScalaSdkDescriptor]] = {
    val chooser = new ScalaSdkFilesChooserDescriptor()
    FileChooser.chooseFiles(chooser, parentComponent, null: ComponentManager, null: VirtualFile).thenApply { virtualFiles =>
      val selectedSomeFiles = virtualFiles != null && virtualFiles.nonEmpty
      if (selectedSomeFiles)
        chooser.resultSdkDescriptor
      else None
    }
  }
}
