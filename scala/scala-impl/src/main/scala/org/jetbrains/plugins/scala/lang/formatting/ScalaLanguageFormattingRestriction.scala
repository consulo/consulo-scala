package org.jetbrains.plugins.scala.lang.formatting

import consulo.annotation.component.ExtensionImpl

import consulo.language.codeStyle.LanguageFormattingRestriction
import consulo.module.content.ProjectFileIndex
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

@ExtensionImpl
class ScalaLanguageFormattingRestriction extends LanguageFormattingRestriction {

  override def isFormatterAllowed(context: PsiElement): Boolean = true

  override def isAutoFormatAllowed(context: PsiElement): Boolean = {
    context.getContainingFile match {
      case file: ScFile if !file.isCompiled =>
        val vFile = file.getVirtualFile
        if (vFile == null) {
          true
        } else {
          val project = file.getProject
          val index = ProjectFileIndex.getInstance(project)

          index.isInSourceContent(vFile)
        }
      case _ => true
    }
  }
}
