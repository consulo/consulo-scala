package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.application.progress.DumbProgressIndicator
import consulo.codeEditor.Editor
import consulo.language.Language
import consulo.language.editor.inlay.InlayGroup
import consulo.language.editor.inlay.setting.{ImmediateConfigurable, InlayProviderSettingsModel}
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings
import org.jetbrains.plugins.scala.codeInsight.hints.ScalaTypeHintsSettingsModel.Case
import org.jetbrains.plugins.scala.codeInsight.implicits.{ImplicitHints, ImplicitHintsPass}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.util
import javax.swing.JComponent
import scala.jdk.CollectionConverters.SeqHasAsJava

class ScalaTypeHintsSettingsModel(project: Project) extends InlayProviderSettingsModel(
  true,
  "Scala.ScalaTypeHintsSettingsModel",
  ScalaLanguage.INSTANCE
)  {
  private lazy val insightSettings = ScalaCodeInsightSettings.getInstance()

  //noinspection ScalaExtractStringToBundle
  override def getName: LocalizeValue = LocalizeValue.of("Type Hints")
  override def getGroup: InlayGroup = InlayGroup.TYPES_GROUP

  private lazy val generalSettingsPanel = new GeneralSettingsPanel(
    () => insightSettings.showObviousType, insightSettings.showObviousType = _,
    () => insightSettings.preserveIndents, insightSettings.preserveIndents = _,
    () => insightSettings.presentationLength, insightSettings.presentationLength = _
  )

  override def getComponent: JComponent = generalSettingsPanel.getPanel

  override def getDescription: String = ScalaCodeInsightBundle.message("type.hints.description", ScalaHintsSettings.xRayModeTipText)

  override def getPreviewText: String = {
    if (project.isDefault)
      return null

    """
      |class Person {
      |  val birthYear = 5 + 5
      |
      |  def ageInYear(year: Int) = {
      |    val diff = year - birthYear
      |    math.max(0, diff)
      |  }
      |}
      |""".stripMargin.withNormalizedSeparator.trim
  }

  override def collectData(editor: Editor, psiFile: PsiFile): Runnable = {
    val settings = new ScalaHintsSettings.Defaults {
      override def showMethodResultType: Boolean = ShowMethodResultTypeCase.currentValue
      override def showMemberVariableType: Boolean = ShowMemberVariableTypeCase.currentValue
      override def showLocalVariableType: Boolean = ShowLocalVariableTypeCase.currentValue
      override def showObviousType: Boolean = generalSettingsPanel.showObviousTypes
      override def preserveIndents: Boolean = generalSettingsPanel.getPreserveIntends
      override def presentationLength: Int = generalSettingsPanel.getPresentationLength
    }
    val previewPass = new ImplicitHintsPass(editor, psiFile.asInstanceOf[ScalaFile], settings, isPreviewPass = true)
    previewPass.doCollectInformation(DumbProgressIndicator.INSTANCE)
    () => previewPass.doApplyInformationToEditor()
  }


  override def isModified: Boolean =
    generalSettingsPanel.isModified ||
      cases.values.exists(_.isModified)

  override def apply(): Unit = {
    generalSettingsPanel.saveSettings()
    cases.values.foreach(_.save())
    ImplicitHints.updateInAllEditors()
  }

  override def reset(): Unit = {
    generalSettingsPanel.reset()
    cases.values.foreach(_.reset())
  }


  override def getCases: util.List[ImmediateConfigurable.Case] =
    cases.keysIterator.toSeq.asJava

  override def getCaseDescription(aCase: ImmediateConfigurable.Case): String =
    cases.get(aCase).map(_.description).orNull

  override def getCasePreview(aCase: ImmediateConfigurable.Case): String =
    cases.get(aCase).map(_.preview).orNull

  override def getCasePreviewLanguage(aCase: ImmediateConfigurable.Case): Language =
    ScalaLanguage.INSTANCE

  private val cases =
    Seq(ShowMemberVariableTypeCase, ShowLocalVariableTypeCase, ShowMethodResultTypeCase)
      .map(c => c.configCase -> c)
      .toMap

  private lazy val ShowMethodResultTypeCase = new Case {
    override def name: String = ScalaCodeInsightBundle.message("method.results")
    override def id: String = "Scala.ScalaTypeHintsSettingsModel.showMethodResultType"
    override def loadSetting(): Boolean = insightSettings.showFunctionReturnType
    override def saveSetting(value: Boolean): Unit = insightSettings.showFunctionReturnType = value
    override def description: String = ScalaCodeInsightBundle.message("method.results.description", ScalaHintsSettings.xRayModeTipText)
    override def preview: String = getPreviewText
  }

  private lazy val ShowLocalVariableTypeCase = new Case {
    override def name: String = ScalaCodeInsightBundle.message("local.variables")
    override def id: String = "Scala.ScalaTypeHintsSettingsModel.showLocalVariableType"
    override def loadSetting(): Boolean = insightSettings.showLocalVariableType
    override def saveSetting(value: Boolean): Unit = insightSettings.showLocalVariableType = value
    override def description: String = ScalaCodeInsightBundle.message("local.variables.description", ScalaHintsSettings.xRayModeTipText)
    override def preview: String = getPreviewText
  }

  private lazy val ShowMemberVariableTypeCase = new Case {
    override def name: String = ScalaCodeInsightBundle.message("member.variables")
    override def id: String = "Scala.ScalaTypeHintsSettingsModel.showMemberVariableType"
    override def loadSetting(): Boolean = insightSettings.showPropertyType
    override def saveSetting(value: Boolean): Unit = insightSettings.showPropertyType = value
    override def description: String = ScalaCodeInsightBundle.message("member.variables.description", ScalaHintsSettings.xRayModeTipText)
    override def preview: String = getPreviewText
  }
}

object ScalaTypeHintsSettingsModel {
  abstract class Case {
    @Nls
    def name: String
    def id: String
    @Nls
    def description: String
    def preview: String = null

    protected def loadSetting(): Boolean
    protected def saveSetting(value: Boolean): Unit

    def isModified: Boolean = currentValue != loadSetting()
    def save(): Unit = saveSetting(currentValue)
    def reset(): Unit = currentValue = loadSetting()

    var currentValue: Boolean = loadSetting()

    final lazy val configCase: ImmediateConfigurable.Case = new ImmediateConfigurable.Case(
      LocalizeValue.of(name), id,
      () => currentValue,
      (newValue: java.lang.Boolean) => currentValue = newValue,
      LocalizeValue.of(description)
    )
  }

  def navigateTo(project: Project): Unit = navigateToInlaySettings[ScalaTypeHintsSettingsModel](project)
}
