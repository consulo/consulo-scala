package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.document.Document
import consulo.project.Project
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile


final class ScalaAccessCanBeTightenedPass(file: ScalaFile, doc: Option[Document])
  extends InspectionBasedHighlightingPass(file, doc, ScalaAccessCanBeTightenedPass.inspection(file.getProject))

object ScalaAccessCanBeTightenedPass {
  def inspection(project: Project): ScalaAccessCanBeTightenedInspection =
    Option(InspectionProjectProfileManager.getInstance(project))
      .map(_.getCurrentProfile)
      .flatMap(profile => Option(profile.getInspectionTool("ScalaWeakerAccess", project)))
      .map(_.getTool.asInstanceOf[ScalaAccessCanBeTightenedInspection])
      .getOrElse(new ScalaAccessCanBeTightenedInspection)
}
