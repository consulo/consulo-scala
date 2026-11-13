package org.jetbrains.plugins.scalaDirective.codeInspection.dependencies

import consulo.language.Language
import consulo.language.editor.inspection.LocalQuickFix
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.packagesearch.codeInspection.DependencyVersionInspection
import org.jetbrains.plugins.scala.packagesearch.codeInspection.DependencyVersionInspection.DependencyDescriptor
import org.jetbrains.plugins.scalaDirective.dependencies.{ScalaDirectiveDependencyDescriptor, ScalaDirectiveDependencyPattern}
import org.jetbrains.plugins.scalaDirective.util.ScalaDirectiveValueKind

final class ScalaDirectiveDependencyVersionInspection extends DependencyVersionInspection with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.directive.dependency.version"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.directive"))

  override def getGroupPath: Array[LocalizeValue] = Array(LocalizeValue.of("Scala"), getGroupDisplayName)

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WEAK_WARNING

  override def getLanguage: Language = ScalaDirectiveLanguage.INSTANCE

  override def getShortName: String = "ScalaDirectiveDependencyVersionInspection"

  override def isEnabledByDefault: Boolean = true

  override protected def isAvailable(element: PsiElement): Boolean =
    ScalaDirectiveDependencyPattern.accepts(element)

  override protected def createDependencyDescriptor(element: PsiElement): Option[DependencyDescriptor] = element match {
    case ScalaDirectiveValueKind(ScalaDirectiveDependencyDescriptor(descriptor), _) => Some(descriptor)
    case _ => None
  }

  override protected def createQuickFix(element: PsiElement, newerVersion: String): LocalQuickFix =
    new ScalaDirectiveUpdateDependencyVersionQuickFix(element, newerVersion)
}
