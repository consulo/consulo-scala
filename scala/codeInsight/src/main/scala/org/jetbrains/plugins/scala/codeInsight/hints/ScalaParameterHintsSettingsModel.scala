package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.language.Language
import consulo.language.editor.inlay.InlayGroup
import consulo.language.editor.inlay.setting.{ImmediateConfigurable, InlayProviderSettingsModel}
import consulo.language.editor.localize.LanguageEditorLocalize
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.Hyperlink
import consulo.ui.ex.awtUnsafe.TargetAWT
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings.{getInstance => ScalaCodeInsightSettings}
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

import java.util
import javax.swing.JComponent

class ScalaParameterHintsSettingsModel(project: Project)
  extends InlayProviderSettingsModel(true, "Scala.ParameterHintsSettingsModel", ScalaLanguage.INSTANCE) {

  override def getGroup: InlayGroup = InlayGroup.PARAMETERS_GROUP

  override def getCases: util.List[ImmediateConfigurable.Case] = util.Collections.emptyList()

  override def getName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("parameter.name.hints"))

  private lazy val component: JComponent =
    TargetAWT.to(Hyperlink.create(LanguageEditorLocalize.settingsInlayJavaExcludeList(),
      _ => new ExcludeListDialog(ScalaLanguage.INSTANCE, ScalaInlayParameterHintsProvider).show())).asInstanceOf[JComponent]

  override def getComponent: JComponent = component

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
