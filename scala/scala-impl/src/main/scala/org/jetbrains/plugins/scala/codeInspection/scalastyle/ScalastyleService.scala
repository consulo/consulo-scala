package org.jetbrains.plugins.scala.codeInspection.scalastyle

import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.editor.inspection.ProblemDescriptor
import consulo.application.ApplicationManager
import consulo.language.psi.PsiFile

trait ScalastyleService {

  def checkFile(file: PsiFile, manager: InspectionManager): Array[ProblemDescriptor]

  def settingsFor(file: PsiFile): ScalastyleSettings
}

object ScalastyleService {
  def instance: ScalastyleService =
    ApplicationManager.getApplication.getInstance(classOf[ScalastyleService])
}
