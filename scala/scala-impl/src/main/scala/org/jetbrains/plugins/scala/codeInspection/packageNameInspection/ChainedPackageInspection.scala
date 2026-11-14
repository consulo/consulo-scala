package org.jetbrains.plugins.scala.codeInspection.packageNameInspection

import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.CodeInsightColors
import consulo.colorScheme.TextAttributesKey
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemDescriptor, ProblemHighlightType}
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

@ExtensionImpl
class ChainedPackageInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.chained.package.clause.inspection"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getEditorAttributesKey: TextAttributesKey = CodeInsightColors.INFO_ATTRIBUTES

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import ChainedPackageInspection._

  override def isEnabledByDefault = true

  override def getID = "ScalaChainedPackageClause"

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] =
    file match {
      case file: ScalaFile if IntentionAvailabilityChecker.checkInspection(this, file) =>
        val maybeProblemDescriptor = for {
          firstPackaging <- file.firstPackaging
          if firstPackaging.isVisible(manager.getProject, file)
          module <- file.module
          basePackage = ScalaProjectSettings.getInstance(file.getProject).getBasePackageFor(module)
          if basePackage.nonEmpty && firstPackaging.packageName != basePackage && (firstPackaging.packageName + ".").startsWith(basePackage + ".")
          reference <- firstPackaging.reference
          range = new TextRange(reference.getTextRange.getStartOffset, reference.getTextRange.getStartOffset + basePackage.length)
          quickFix = new UseChainedPackageQuickFix(file)
        } yield manager.createProblemDescriptor(
          file,
          range,
          ScalaInspectionBundle.message("package.declaration.could.use.chained.package.clauses", basePackage),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
          false,
          quickFix
        )

        maybeProblemDescriptor match {
          case Some(problemDescriptor) => Array(problemDescriptor)
          case _ => ProblemDescriptor.EMPTY_ARRAY
        }
      case _ => ProblemDescriptor.EMPTY_ARRAY
    }
}

object ChainedPackageInspection {

  private class UseChainedPackageQuickFix(myFile: ScalaFile)
    extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("use.chained.package.clauses.like"), myFile) {

    override protected def doApplyFix(file: ScalaFile)(implicit project: Project): Unit =
      file.setPackageName(file.getPackageName)

    override def getFamilyName: String = ScalaInspectionBundle.message("use.chained.package.clauses")
  }
}
