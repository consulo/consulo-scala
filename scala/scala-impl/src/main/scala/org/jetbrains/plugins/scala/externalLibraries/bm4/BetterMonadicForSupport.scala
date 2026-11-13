package org.jetbrains.plugins.scala.externalLibraries.bm4

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Inject
import jakarta.inject.Singleton

import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ScalaFeatures

/**
  * See: https://github.com/oleg-py/better-monadic-for
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
class BetterMonadicForSupport @Inject()(project: Project) {
  private val implicit0: PsiElement = {
    val text =
    """
      |object implicit0 {
      | def unapply[A](a: A): Option[A] = ???
      |}
    """.stripMargin


    val file = ScalaPsiElementFactory.createScalaFileFromText(text, ScalaFeatures.default)(using project)
    file.typeDefinitions.head
  }

  val syntheticDeclarations: Seq[PsiElement] = Seq(
    implicit0
  )
}

object BetterMonadicForSupport {
  def apply(project: Project): BetterMonadicForSupport =
    project.getInstance(classOf[BetterMonadicForSupport])
}
