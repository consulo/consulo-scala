package org.jetbrains.plugins.scala.testingSupport.locationProvider

import consulo.execution.action.PsiLocation
import consulo.project.Project
import consulo.language.psi.PsiElement

case class PsiLocationWithName[T <: PsiElement](
  project: Project,
  element: T,
  name: String
) extends PsiLocation[T](project, element)

object PsiLocationWithName {
  def apply[T <: PsiElement](element: T, name: String): PsiLocationWithName[T] =
    new PsiLocationWithName(element.getProject, element, name)
}