package org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n.internal

import consulo.application.ApplicationManager
import consulo.language.Language
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.language.psi.PsiElementVisitor
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.scalai18n.ScalaI18nBundle

abstract class ScalaI18nInternalInspection(displayNameKey: String, level: HighlightDisplayLevel) extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaI18nBundle.message(displayNameKey))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.internal"))

  override def getGroupPath: Array[LocalizeValue] = Array(LocalizeValue.of("Scala"), getGroupDisplayName)

  override def getDefaultLevel: HighlightDisplayLevel = level

  override def isEnabledByDefault: Boolean = true

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  // upstream: registered with isInternal="true" (Consulo has no internal inspections)
  override final def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    if (ScalaI18nInternalInspection.isEnabledIn(holder.getProject)) buildInternalVisitor(holder, isOnTheFly)
    else PsiElementVisitor.EMPTY_VISITOR

  protected def buildInternalVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor
}

object ScalaI18nInternalInspection {
  def isEnabledIn(project: Project): Boolean =
    ApplicationManager.getApplication.isInternal || project.isIntellijScalaPluginProject
}
