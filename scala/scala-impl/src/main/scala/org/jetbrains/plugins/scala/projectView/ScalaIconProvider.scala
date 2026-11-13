package org.jetbrains.plugins.scala.projectView

import consulo.ui.image.Image
import com.intellij.ide.IconProvider
import consulo.application.progress.ProgressManager
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile


final class ScalaIconProvider extends IconProvider {

  override def getIcon(element: PsiElement, flags: Int): Image = element match {
    case file: ScalaFile =>
      ProgressManager.checkCanceled()
      Node(file)(using null, null).getIcon(flags)
    case _ => null
  }
}