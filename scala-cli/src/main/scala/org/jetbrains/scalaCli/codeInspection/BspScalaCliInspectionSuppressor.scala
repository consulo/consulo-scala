package org.jetbrains.scalaCli.codeInspection

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{InspectionSuppressor, SuppressQuickFix}
import consulo.language.psi.PsiElement
import org.jetbrains.bsp.BspUtil
import org.jetbrains.plugins.scala.codeInspection.packageNameInspection.ScalaPackageNameInspection
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

@ExtensionImpl
final class BspScalaCliInspectionSuppressor extends InspectionSuppressor {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  private val suppressedToolIds: Set[String] = Set(
    ScalaPackageNameInspection.ToolId
  )

  override def isSuppressedFor(element: PsiElement, toolId: String): Boolean =
    element match {
      case scalaFile: ScFile =>
        suppressedToolIds.contains(toolId) && BspUtil.isBspScalaCliProject(scalaFile.getProject)
      case _ =>
        false
    }

  override def getSuppressActions(element: PsiElement, toolId: String): Array[SuppressQuickFix] = Array()
}