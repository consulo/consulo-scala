package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.ExtensionImpl

import java.{util => ju}
import com.intellij.codeInsight.hints.settings.{InlayProviderSettingsModel, InlaySettingsProvider}
import consulo.language.Language
import consulo.project.Project
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.hints.methodChains.ScalaMethodChainInlayHintsSettingsModel
import org.jetbrains.plugins.scala.codeInsight.hints.rangeHints.{ExclusiveRangeHintSettingsModel, RangeHintsForToAndUntilSettingsModel}

@ExtensionImpl
class ScalaInlayHintsSettingsProvider extends InlaySettingsProvider {
  override def createModels(project: Project, language: Language): ju.List[InlayProviderSettingsModel] =
    if (language == ScalaLanguage.INSTANCE) {
      ju.Arrays.asList(
        new ScalaParameterHintsSettingsModel(project),
        new ScalaTypeHintsSettingsModel(project),
        new ScalaMethodChainInlayHintsSettingsModel(project),
        new RangeHintsForToAndUntilSettingsModel(project),
        new ExclusiveRangeHintSettingsModel(project),
      )
    }
    else ju.Collections.emptyList()

  override def getSupportedLanguages(project: Project): ju.Collection[Language] =
    ju.Collections.singletonList(ScalaLanguage.INSTANCE)
}
