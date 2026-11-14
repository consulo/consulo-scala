package org.jetbrains.sbt.project.modifier.location

import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.sbt.project.modifier.BuildFileElementType

object EndOfFileLocationProvider extends BuildFileModificationLocationProvider {
  override def findLocationInFile(file: PsiFile, module: consulo.module.Module, elementType: BuildFileElementType,
                                  elementCondition: Option[PsiElement => Boolean]): Option[(PsiElement, Int)] = {
    elementCondition match {
      case Some(_) => None
      case None => Some(file, file.getChildren.length)
    }
  }
}
