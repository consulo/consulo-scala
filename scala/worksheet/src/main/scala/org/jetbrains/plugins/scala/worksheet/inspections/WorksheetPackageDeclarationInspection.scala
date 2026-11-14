package org.jetbrains.plugins.scala.worksheet.inspections

import consulo.annotation.component.ExtensionImpl
import consulo.application.dumb.DumbAware
import consulo.language.Language
import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemDescriptor, ProblemHighlightType}
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.worksheet.{WorksheetBundle, WorksheetFile}

@ExtensionImpl
final class WorksheetPackageDeclarationInspection extends LocalInspectionTool with DumbAware {

  override def getShortName: String = "WorksheetPackageDeclaration"

  override def getDisplayName: LocalizeValue = LocalizeValue.of(WorksheetBundle.message("displayname.worksheet.package.declaration"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(WorksheetBundle.message("group.worksheet"))

  override def getGroupPath: Array[LocalizeValue] = Array(LocalizeValue.of("Scala"), getGroupDisplayName)

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.ERROR

  override def isEnabledByDefault: Boolean = true

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] =
    file match {
      case file: WorksheetFile =>
        file.packagingRanges.map { range =>
          manager.newProblemDescriptor(LocalizeValue.of(WorksheetBundle.message("package.declarations.are.not.allowed.in.worksheets")))
            .range(file, range)
            .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
            .onTheFly(isOnTheFly)
            .create()
        }.toArray
      case _ => null
    }
}
