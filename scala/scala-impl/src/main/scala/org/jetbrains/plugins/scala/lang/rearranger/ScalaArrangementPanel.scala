package org.jetbrains.plugins.scala.lang.rearranger

import consulo.language.codeStyle.ui.setting.arrangement.ArrangementSettingsPanel
import consulo.language.codeStyle.CodeStyleSettings
import org.jetbrains.plugins.scala.api.{ScalaFileType, ScalaLanguage}

class ScalaArrangementPanel(settings: CodeStyleSettings) extends ArrangementSettingsPanel(settings, ScalaLanguage.INSTANCE) {

    override protected def getRightMargin: Int = 80

    override protected def getFileType: ScalaFileType = ScalaFileType.INSTANCE

    override protected def getPreviewText: String = null
}
