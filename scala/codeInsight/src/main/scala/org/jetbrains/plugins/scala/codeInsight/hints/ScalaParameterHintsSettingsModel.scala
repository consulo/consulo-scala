package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.language.editor.CodeInsightBundle
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.{ImmediateConfigurable, InlayGroup}
import consulo.language.Language
import consulo.project.Project
import com.intellij.ui.components.ActionLink
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings.{getInstance => ScalaCodeInsightSettings}
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints

import java.awt.event.ActionListener
import java.util
import javax.swing.JComponent

class ScalaParameterHintsSettingsModel(project: Project)
  extends InlayProviderSettingsModel(true, "Scala.ParameterHintsSettingsModel", ScalaLanguage.INSTANCE) {

  override def getGroup: InlayGroup = InlayGroup.PARAMETERS_GROUP

  override def getCases: util.List[ImmediateConfigurable.Case] = util.Collections.emptyList()

  override def getMainCheckBoxLabel: String = ScalaCodeInsightBundle.message("parameter.name.hints.action.description")

  override def getName: String = ScalaCodeInsightBundle.message("parameter.name.hints")

  override def getComponent: JComponent =
    new ActionLink(CodeInsightBundle.message("settings.inlay.java.exclude.list"),
      (_ => new ExcludeListDialog(ScalaLanguage.INSTANCE, ScalaInlayParameterHintsProvider).show()): ActionListener)

  override def getPreviewText: String = null

  override def apply(): Unit = {
    ScalaCodeInsightSettings.showParameterNames = isEnabled
    ImplicitHints.updateInAllEditors()
  }

  override def isModified: Boolean = isEnabled != ScalaCodeInsightSettings.showParameterNames

  override def reset(): Unit = setEnabled(ScalaCodeInsightSettings.showParameterNames)

  override def getDescription: String = ScalaCodeInsightBundle.message("parameter.name.hints.description", ScalaHintsSettings.xRayModeTipText)

  override def getCaseDescription(aCase: ImmediateConfigurable.Case): String = null

  override def getCasePreview(aCase: ImmediateConfigurable.Case): String = null

  override def getCasePreviewLanguage(aCase: ImmediateConfigurable.Case): Language = ScalaLanguage.INSTANCE
}

object ScalaParameterHintsSettingsModel {
  def navigateTo(project: Project): Unit = navigateToInlaySettings[ScalaParameterHintsSettingsModel](project)
}