package org.jetbrains.plugins.scala.projectView

import consulo.annotation.component.ExtensionImpl

import consulo.application.progress.ProgressManager
import consulo.language.icon.{IconDescriptor, IconDescriptorUpdater}
import consulo.language.psi.PsiElement
import consulo.component.util.Iconable
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile


// upstream: com.intellij.ide.IconProvider, Consulo uses IconDescriptorUpdater;
// after java: consulo.java's updater sets Java class icons for every PsiClass, Scala classes included
@ExtensionImpl(order = "after java")
final class ScalaIconProvider extends IconDescriptorUpdater {

  override def updateIcon(descriptor: IconDescriptor, element: PsiElement, flags: Int): Unit = element match {
    case file: ScalaFile =>
      ProgressManager.checkCanceled()
      val icon = Node(file)(using null, null).getIcon(flags)
      if (icon != null) {
        descriptor.setMainIcon(icon)
      }
    // Scala PSI elements with own icons (upstream: PsiElement.getIcon/ElementBase)
    case iconable: Iconable if element.getLanguage.isKindOf(ScalaLanguage.INSTANCE) =>
      val icon = iconable.getIcon(flags)
      if (icon != null) {
        descriptor.setMainIcon(icon)
      }
    case _ =>
  }
}
