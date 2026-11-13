package org.jetbrains.plugins.scala.codeInsight.navigation

import consulo.application.Application
import consulo.language.editor.ui.navigation.{PsiTargetPresentationFactory, TargetPresentationProvider}
import consulo.language.psi.PsiElement
import consulo.localize.LocalizeValue
import consulo.navigation.TargetPresentation
import consulo.ui.image.Image

// upstream: PsiTargetPresentationRenderer delegating to com.intellij.ide.util.PsiElementRenderingInfo
class DelegatingPsiTargetPresentationRenderer[T <: PsiElement](private val delegate: TargetPresentationProvider[? >: T]) extends TargetPresentationProvider[T] {
  def getElementText(element: T): String = delegate.getPresentation(element).getPresentableText.get

  def getContainerText(element: T): String = delegate.getPresentation(element).getContainerText match {
    case text if text.isEmpty => null
    case text => text.get
  }

  def getIcon(element: T): Image = delegate.getPresentation(element).getIcon

  override def getPresentation(element: T): TargetPresentation = {
    val containerText = getContainerText(element)
    Application.get().getInstance(classOf[PsiTargetPresentationFactory])
      .presentationBuilder(element)
      .withPresentableText(LocalizeValue.of(getElementText(element)))
      .withIcon(getIcon(element))
      .withContainerText(if (containerText == null) LocalizeValue.empty() else LocalizeValue.of(containerText))
      .build()
  }
}
