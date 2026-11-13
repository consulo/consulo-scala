package org.jetbrains.plugins.scala.codeInspection.scalastyle

import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemDescriptor}
import consulo.language.psi.PsiFile

class ScalastyleCodeInspection extends LocalInspectionTool {
  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    ScalastyleService.instance.checkFile(file, manager)
  }
}
