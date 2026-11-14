package org.jetbrains.plugins.scala
package codeInsight
package generation
package actions

import com.intellij.java.impl.codeInsight.generation.actions.BaseGenerateAction
import consulo.localize.LocalizeValue
import consulo.codeEditor.Editor
import consulo.project.Project
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.PsiFile
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition

abstract class ScalaBaseGenerateAction(handler: ScalaCodeInsightActionHandler,
                                       @Nls actionText: String,
                                       @Nls actionDescription: String) extends BaseGenerateAction(handler, LocalizeValue.of(actionText)) {
  getTemplatePresentation.setDescription(LocalizeValue.of(actionDescription))

  override protected def isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean =
    file match {
      case scalaFile: ScalaFile if scalaFile.isWritable =>
        handler.isValidFor(editor, file) && targetClass(using editor, scalaFile).exists(isValidForClass)
      case _ => false
    }

  override protected def isValidForClass(targetClass: PsiClass): Boolean = true

  override protected def getTargetClass(editor: Editor, file: PsiFile): PsiClass =
    targetClass(using editor, file).orNull

  private def targetClass(implicit editor: Editor, file: PsiFile) =
    elementOfTypeAtCaret(classOf[ScTemplateDefinition])
}

