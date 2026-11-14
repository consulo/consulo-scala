package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.Configurable
import consulo.language.Language
import consulo.language.codeStyle.CodeStyleSettings
import consulo.language.codeStyle.setting.CodeStyleSettingsProvider
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.{NlsString, ScalaLanguage}
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaLanguageCodeStyleSettingsProvider.ScalaCodeStyleAbstractConfigurable

/**
 * upstream: LanguageCodeStyleSettingsProvider is a CodeStyleSettingsProvider, in Consulo the code style pages come from
 * CodeStyleSettingsProvider only (like consulo-java's JavaCodeStyleSettingsProvider next to its language provider)
 */
@ExtensionImpl
final class ScalaCodeStyleSettingsProvider extends CodeStyleSettingsProvider {

  override def createSettingsPage(baseSettings: CodeStyleSettings, modelSettings: CodeStyleSettings): Configurable =
    new ScalaCodeStyleAbstractConfigurable(baseSettings, modelSettings)

  override def getConfigurableDisplayName: LocalizeValue = LocalizeValue.of(NlsString.displayNameAndConfigurableId("Scala"))

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def createCustomSettings(settings: CodeStyleSettings): ScalaCodeStyleSettings = new ScalaCodeStyleSettings(settings)
}
