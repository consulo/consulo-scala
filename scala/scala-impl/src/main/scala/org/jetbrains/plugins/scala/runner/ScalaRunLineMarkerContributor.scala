package org.jetbrains.plugins.scala.runner

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.ui.image.Image
import consulo.execution.lineMarker.RunLineMarkerContributor.Info
import consulo.execution.lineMarker.{ExecutorAction, RunLineMarkerContributor}
import consulo.execution.icon.ExecutionIconGroup
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.incremental.Highlighting.ElementHighlightingExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.runner.ScalaRunLineMarkerContributor.RunIcon
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil


@ExtensionImpl(id = "ScalaRunLineMarkerContributor")
class ScalaRunLineMarkerContributor extends RunLineMarkerContributor {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def getInfo(element: PsiElement): Info = {
    val file = element.getContainingFile
    val project = if (file != null) file.getProject else element.getProject // Avoid tree walk-up

    if (!ScalaProjectSettings.in(project).isDisableInspections) {
      if (!element.isVisible(project, file)) return null
    }

    file match {
      case scriptLikeFile: ScalaFile
        if scriptLikeFile.isWorksheetFile || scriptLikeFile.isMultipleDeclarationsAllowed =>
        return null
      case _ =>
    }

    if (ScalaMainMethodUtil.hasMain(element)) {
      val actions = ExecutorAction.getActions(0)
      new Info(RunIcon, (e: PsiElement) => actions.flatMap(a => Option(RunLineMarkerContributor.getText(a, e))).mkString("\n"), actions*)
    } else
      null
  }
}

object ScalaRunLineMarkerContributor {
  val RunIcon: Image = ExecutionIconGroup.gutterRun()
}
