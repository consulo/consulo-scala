package org.jetbrains.plugins.scala.projectView

import com.intellij.ide.IconProvider
import consulo.application.progress.ProgressManager
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import javax.swing.Icon

final class ScalaIconProvider extends IconProvider {

  override def getIcon(element: PsiElement, flags: Int): Icon = element match {
    case file: ScalaFile =>
      ProgressManager.checkCanceled()
      Node(file)(using null, null).getIcon(flags)
    case _ => null
  }
}