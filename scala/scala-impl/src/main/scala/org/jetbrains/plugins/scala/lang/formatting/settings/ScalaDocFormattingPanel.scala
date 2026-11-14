package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.localize.LocalizeValue

import consulo.language.codeStyle.ui.setting.OptionTreeWithPreviewPanel
import consulo.virtualFileSystem.fileType.FileType
import consulo.ui.ex.awt.OnePixelDivider
import consulo.language.codeStyle.CodeStyleSettings
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider
import consulo.ui.ex.awt.CustomLineBorder
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaDocFormattingPanel.ScalaDocCodeSample
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.{ScalaFileType, ScalaLanguage}

import consulo.ui.CheckBox
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{DockLayout, VerticalLayout}

import javax.swing.JComponent

class ScalaDocFormattingPanel(val settings: CodeStyleSettings) extends OptionTreeWithPreviewPanel(settings) {

  private var myEnableCheckBox: CheckBox = scala.compiletime.uninitialized
  private var myAsteriskStyleCheckBox: CheckBox = scala.compiletime.uninitialized
  private var myScaladocPanel: JComponent = scala.compiletime.uninitialized

  init()

  override def getSettingsType = LanguageCodeStyleSettingsProvider.SettingsType.LANGUAGE_SPECIFIC

  override def getPanel: JComponent = myScaladocPanel

  override protected def getTabTitle: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scaladoc.panel.title"))

  override protected def getFileType: FileType = ScalaFileType.INSTANCE

  override protected def getRightMargin: Int = 47

  override protected def getPreviewText: String = ScalaDocCodeSample

  override def init(): Unit = {
    super.init()

    myEnableCheckBox = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scaladoc.panel.enable.scaladoc.formatting")))
    myEnableCheckBox.addValueListener(_ => update())
    myAsteriskStyleCheckBox = CheckBox.create(LocalizeValue.of(ScalaBundle.message("scaladoc.panel.add.additional.space.for.leading.asterisk")))
    val topPanel = VerticalLayout.create().add(myEnableCheckBox).add(myAsteriskStyleCheckBox)

    myPanel.setBorder(new CustomLineBorder(OnePixelDivider.BACKGROUND, 1, 0, 0, 0))

    // the options tree with the preview is the platform Swing panel
    myScaladocPanel = TargetAWT.to(DockLayout.create().top(topPanel).center(TargetAWT.wrap(myPanel))).asInstanceOf[JComponent]
  }

  override protected  def initTables(): Unit = {
    initCustomOptions(ScalaDocFormattingPanel.ALIGNMENT_GROUP)
    initCustomOptions(ScalaDocFormattingPanel.BLANK_LINES_GROUP)
    initCustomOptions(ScalaDocFormattingPanel.OTHER_GROUP)
  }

  private def update(): Unit = {
    setEnabled(myPanel, myEnableCheckBox.getValue)
    myAsteriskStyleCheckBox.setEnabled(true)
  }

  private def setEnabled(c: JComponent, enabled: Boolean): Unit = {
    c.setEnabled(enabled)
    val children = c.getComponents
    for (child <- children) {
      child match {
        case c1: JComponent =>
          setEnabled(c1, enabled)
        case _ =>
      }
    }
  }

  override def apply(settings: CodeStyleSettings): Unit = {
    super.apply(settings)
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    scalaSettings.ENABLE_SCALADOC_FORMATTING = myEnableCheckBox.getValue
    scalaSettings.USE_SCALADOC2_FORMATTING = myAsteriskStyleCheckBox.getValue
  }

  override protected def resetImpl(settings: CodeStyleSettings): Unit = {
    super.resetImpl(settings)
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    myEnableCheckBox.setValue(scalaSettings.ENABLE_SCALADOC_FORMATTING)
    myAsteriskStyleCheckBox.setValue(scalaSettings.USE_SCALADOC2_FORMATTING)
    update()
  }

  override def isModified(settings: CodeStyleSettings): Boolean = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    super.isModified(settings) || myEnableCheckBox.getValue != scalaSettings.ENABLE_SCALADOC_FORMATTING ||
      myAsteriskStyleCheckBox.getValue != scalaSettings.USE_SCALADOC2_FORMATTING
  }

  protected override def customizeSettings(): Unit = {
    val provider: LanguageCodeStyleSettingsProvider = LanguageCodeStyleSettingsProvider.forLanguage(ScalaLanguage.INSTANCE)
    if (provider != null) {
      provider.customizeSettings(this, getSettingsType)
    }
  }
}

object ScalaDocFormattingPanel {

  val BLANK_LINES_GROUP: String = ScalaBundle.message("scaladoc.panel.groups.blank.lines")
  val ALIGNMENT_GROUP  : String = ScalaBundle.message("scaladoc.panel.groups.alignment")
  val OTHER_GROUP      : String = ScalaBundle.message("scaladoc.panel.groups.other")

  private val ScalaDocCodeSample =
    """
      |/**
      |  *   Some description
      |  *
      |  * Lists:
      |  *  1. item 1 line 1
      |  * item 1 line 2
      |  * item 1 line 3
      |  *    I.      item inner 1 line 1
      |  *            item inner 1 line 2
      |  *    I. item inner 2 line 1
      |  *  1. item 2
      |  *      - item inner 3
      |  *      - item inner 4 line 1
      |  * item inner 4 line 2
      |  *
      |  *Some other description
      |  * @note Note that this tag is here just to show
      |  * how exactly alignment for tags different from parameters and return tags
      |
      |
      |  * @forExample   Even if the tag is not valid, formatting will still be fine
      |  *   also, if you choose to preserver spaces in tags, no spaces will be removed after tag value
      |  * @param x Some parameter named x that has
      |  * a multiline description
      |  * @param yy Another parameter named yy
      |  * @param longParamName Another parameter with a long name
      |  * @tparam A description of parameter A
      |  * with very long body
      |  * @tparam B description of parameter B
      |  * also with very
      |  * long body
      |  * @return Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
      |  *         eiusmod tempor incididunt ut labore et dolore magna aliqua.
      |
      |  * @throws RuntimeException whenever it feels like it
      |
      |
      |  * @throws IndexOutOfBoundsException when index is out of bound
      |  */
      |def foo[A, B](x: Int, yy: Int, longParamName: Int): Int
      |""".stripMargin.replace("\r", "")

}