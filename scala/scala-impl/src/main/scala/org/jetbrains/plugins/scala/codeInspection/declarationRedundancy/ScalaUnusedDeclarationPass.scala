package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.document.Document
import consulo.project.Project
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

final class ScalaUnusedDeclarationPass(file: ScalaFile, doc: Option[Document])
  extends InspectionBasedHighlightingPass(file, doc, ScalaUnusedDeclarationPass.inspection(file.getProject))

object ScalaUnusedDeclarationPass {
  def inspection(project: Project): ScalaUnusedDeclarationInspection =
    Option(InspectionProjectProfileManager.getInstance(project))
      .map(_.getCurrentProfile)
      .flatMap(profile => Option(profile.getInspectionTool("ScalaUnusedSymbol", project)))
      .map(_.getTool.asInstanceOf[ScalaUnusedDeclarationInspection])
      .getOrElse(new ScalaUnusedDeclarationInspection)
}
