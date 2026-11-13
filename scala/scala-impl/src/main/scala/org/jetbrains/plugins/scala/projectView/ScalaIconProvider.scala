package org.jetbrains.plugins.scala.projectView

import consulo.application.progress.ProgressManager
import consulo.language.icon.{IconDescriptor, IconDescriptorUpdater}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile


// upstream: com.intellij.ide.IconProvider, Consulo uses IconDescriptorUpdater
final class ScalaIconProvider extends IconDescriptorUpdater {

  override def updateIcon(descriptor: IconDescriptor, element: PsiElement, flags: Int): Unit = element match {
    case file: ScalaFile =>
      ProgressManager.checkCanceled()
      val icon = Node(file)(using null, null).getIcon(flags)
      if (icon != null) {
        descriptor.setMainIcon(icon)
      }
    case _ =>
  }
}
