package org.jetbrains.plugins.scala.annotator.gutter

import consulo.application.Application
import consulo.language.editor.ui.navigation.{PsiTargetNavigationService, TargetPresentationProvider}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.language.psi.{NavigatablePsiElement, PsiElement}
import consulo.ui.event.ComponentEvent
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.annotator.gutter.ScalaMarkerType.newPresentationProvider

import java.util.Arrays.asList
import java.util.function.Supplier
import java.{util => ju}

private[gutter]
object ScalaNavigationUtils {

  def navigate[T <: PsiElement](
    event: ComponentEvent[?],
    targets: Array[T],
    project: Project,
    @Nls title: String,
    @Nls tabTitle: String,
    presentationProvider: TargetPresentationProvider[? >: T]
  ): Unit = {
    val targetsSupplier: Supplier[ju.Collection[T]] = () => asList(targets*)
    Application.get().getInstance(classOf[PsiTargetNavigationService])
      .newNavigator[T](targetsSupplier)
      .presentationProvider(presentationProvider)
      .title(LocalizeValue.of(title))
      .findUsagesTitle(LocalizeValue.of(tabTitle))
      .navigate(event, project)
  }

  def navigateToSuperMember[T <: NavigatablePsiElement](
    event: ComponentEvent[?],
    members: Array[T],
    project: Project,
    @Nls title: String,
    @Nls findUsagesTitle: String,
    presentationProvider: TargetPresentationProvider[? >: T] = newPresentationProvider
  ): Unit = {
    navigate(event, members, project, title, findUsagesTitle, presentationProvider)
  }

  def navigateToSuperType[T <: NavigatablePsiElement](event: ComponentEvent[?], members: Array[T], project: Project, name: String): Unit = {
    val title = ScalaBundle.message("navigation.title.super.types", name)
    val findUsagesTitle = ScalaBundle.message("navigation.findUsages.title.super.types", name)
    navigateToSuperMember(event, members, project, title, findUsagesTitle)
  }

  def navigateToSuperMember[T <: NavigatablePsiElement](event: ComponentEvent[?], members: Array[T], project: Project, name: String): Unit = {
    val title = ScalaBundle.message("navigation.title.super.members", name)
    val findUsagesTitle = ScalaBundle.message("navigation.findUsages.title.super.members", name)
    navigateToSuperMember(event, members, project, title, findUsagesTitle)
  }
}
