package org.jetbrains.plugins.scala.lang.refactoring.util

import consulo.project.DumbService
import consulo.util.dataholder.Key
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.refactoring.Associations

private [refactoring]
object ScalaChangeContextUtil {


  object MovedElementData {

    private val key = Key.create[PsiElement]("moved.element")

    def apply(element: PsiElement): PsiElement = element.getUserData(key)

    def update(element: PsiElement, movedElement: PsiElement): Unit = {
      element.putUserData(key, movedElement)
    }
  }

  def encodeContextInfo(element: PsiElement): Unit = {
    val associations = collectDataForElement(element)
    Associations.Data(element) = associations
  }

  def movedMember(target: PsiElement): PsiElement = {
    val moved = MovedElementData(target)
    MovedElementData(target) = null
    moved
  }

  def collectDataForElement(element: PsiElement): Associations = element.getContainingFile match {
    case scalaFile: ScalaFile if !DumbService.getInstance(scalaFile.getProject).isDumb =>
      element.getTextRange match {
        case range if range.getStartOffset == 0 => null
        case range => Associations.collectAssociations(range)(using scalaFile)
      }
    case _ => null
  }
}
