package org.jetbrains.plugins.scala.codeInspection.scalastyle

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemDescriptor}
import consulo.language.psi.PsiFile

@ExtensionImpl
class ScalastyleCodeInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.scala.style.inspection"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.code.style"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "ScalaStyle"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    ScalastyleService.instance.checkFile(file, manager)
  }
}
