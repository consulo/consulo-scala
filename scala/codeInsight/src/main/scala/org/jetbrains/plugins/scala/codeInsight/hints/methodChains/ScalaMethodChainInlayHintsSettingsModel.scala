package org.jetbrains.plugins.scala.codeInsight.hints.methodChains

import consulo.codeEditor.Editor
import consulo.language.Language
import consulo.language.editor.inlay.InlayGroup
import consulo.language.editor.inlay.setting.{ImmediateConfigurable, InlayProviderSettingsModel}
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.Hyperlink
import consulo.ui.ex.awtUnsafe.TargetAWT
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightSettings
import org.jetbrains.plugins.scala.codeInsight.hints.{ScalaHintsSettings, ScalaTypeHintsSettingsModel, navigateToInlaySettings}
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions.StringExt

import java.awt.{BorderLayout, FlowLayout}
import java.util
import java.util.Collections
import javax.swing.{JComponent, JPanel}

class ScalaMethodChainInlayHintsSettingsModel(project: Project) extends InlayProviderSettingsModel(
  true,
  "Scala.ScalaMethodChainInlayHintsSettingsModel",
  ScalaLanguage.INSTANCE
) {
  override def getGroup: InlayGroup = InlayGroup.METHOD_CHAINS_GROUP

  // have a temporary version of the settings, so apply/cancel mechanism works
  object settings {
    private val global = ScalaCodeInsightSettings.getInstance()
    var alignMethodChainInlayHints: Boolean = scala.compiletime.uninitialized
    var uniqueTypesToShowMethodChains: Int = scala.compiletime.uninitialized

    reset()

    def reset(): Unit = {
      setEnabled(global.showMethodChainInlayHints)
      alignMethodChainInlayHints = global.alignMethodChainInlayHints
      uniqueTypesToShowMethodChains = global.uniqueTypesToShowMethodChains
    }

    def apply(): Unit = {
      global.showMethodChainInlayHints = isEnabled
      global.alignMethodChainInlayHints = alignMethodChainInlayHints
      global.uniqueTypesToShowMethodChains = uniqueTypesToShowMethodChains
    }

    def isModified: Boolean =
      global.showMethodChainInlayHints != isEnabled ||
        global.alignMethodChainInlayHints != alignMethodChainInlayHints ||
        global.uniqueTypesToShowMethodChains != uniqueTypesToShowMethodChains
  }

  override def getCases: util.List[ImmediateConfigurable.Case] = Collections.emptyList()

  private val settingsPanel = new ScalaMethodChainInlaySettingsPanel(
    () => settings.alignMethodChainInlayHints,
    b => {
      settings.alignMethodChainInlayHints = b
      getOnChangeListener.settingsChanged()
    },
    () => settings.uniqueTypesToShowMethodChains,
    i => {
      settings.uniqueTypesToShowMethodChains = i
      getOnChangeListener.settingsChanged()
    }
  )

  override def getName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("method.chain.hints"))

  override def getPreviewText: String = {
    if (project.isDefault)
      return null

    """
      |readSettings()
      |  .server
      |  .connect()
      |  .withHandshakeUpgrade
      |  .ping()
      |
      |def readSettings(): Configuration = ???
      |
      |class Configuration(val server: IPAddress)
      |trait IPAddress { def connect(): Connection }
      |trait Connection {
      |  def withHandshakeUpgrade: Connection
      |  def ping(): Int
      |}
      |""".stripMargin.withNormalizedSeparator.trim
  }

  override def apply(): Unit = {
    settings.apply()
    ImplicitHints.updateInAllEditors()
  }

  // create a dedicated pass for the preview
  private lazy val previewPass: ScalaMethodChainInlayHintsPass = new ScalaMethodChainInlayHintsPass {
    private def globalSettings = ScalaMethodChainInlayHintsSettingsModel.this.settings

    override val settings: ScalaHintsSettings = new ScalaHintsSettings.Defaults {
      override def showMethodChainInlayHints: Boolean = true
      override def alignMethodChainInlayHints: Boolean = globalSettings.alignMethodChainInlayHints
      override def uniqueTypesToShowMethodChains: Int = globalSettings.uniqueTypesToShowMethodChains
      override def showObviousType: Boolean = true // always show obvious types in the preview
    }
  }

  override def collectAndApply(editor: Editor, psiFile: PsiFile): Unit = {
    previewPass.collectMethodChainHints(editor, psiFile)
    previewPass.regenerateMethodChainHints(editor, editor.getInlayModel, psiFile)
  }

  override def isModified: Boolean = settings.isModified

  override def reset(): Unit = {
    settings.reset()
    settingsPanel.reset()
  }

  override def getDescription: String = ScalaCodeInsightBundle.message("method.chain.hints.description", ScalaHintsSettings.xRayModeTipText)

  private lazy val component: JComponent = {
    val linePanel = {
      val link = Hyperlink.create(LocalizeValue.of(ScalaCodeInsightBundle.message("method.chain.hints.link.to.general.settings")),
        _ => ScalaTypeHintsSettingsModel.navigateTo(project))
      val linePanel = {
        val layout = new FlowLayout()
        layout.setHgap(0)
        layout.setAlignment(FlowLayout.LEFT)
        new JPanel(layout)
      }
      linePanel.add(TargetAWT.to(link))
      linePanel
    }

    val panel = new JPanel(new BorderLayout())
    panel.add(settingsPanel.getPanel, BorderLayout.NORTH)
    panel.add(linePanel, BorderLayout.SOUTH)
    panel
  }

  override def getComponent: JComponent = component

  override def getCaseDescription(aCase: ImmediateConfigurable.Case): String = null

  override def getCasePreview(aCase: ImmediateConfigurable.Case): String = null

  override def getCasePreviewLanguage(aCase: ImmediateConfigurable.Case): Language = ScalaLanguage.INSTANCE
}

object ScalaMethodChainInlayHintsSettingsModel {
  def navigateTo(project: Project): Unit = navigateToInlaySettings[ScalaMethodChainInlayHintsSettingsModel](project)
}
