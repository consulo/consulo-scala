package org.jetbrains.plugins.scala.codeInspection.scalastyle

import consulo.annotation.component.ServiceImpl
import consulo.language.editor.inspection.ProblemDescriptor
import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.psi.PsiFile
import jakarta.inject.Singleton

/** Used while the scalastyle integration (upstream integration/scalastyle module) is not ported: no checks. */
@Singleton
@ServiceImpl
final class NoScalastyleService extends ScalastyleService {
  override def checkFile(file: PsiFile, manager: InspectionManager): Array[ProblemDescriptor] = Array.empty

  override def settingsFor(file: PsiFile): ScalastyleSettings = ScalastyleSettings(scalastyleOrder = false, None)
}
