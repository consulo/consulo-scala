package org.jetbrains.plugins.scala.editor.autoimport

import consulo.annotation.component.ExtensionImpl
import consulo.application.ApplicationBundle
import consulo.configurable.{ApplicationConfigurable, SimpleConfigurableByProperties}
import consulo.disposer.Disposable
import consulo.java.impl.JavaBundle
import consulo.language.editor.CodeInsightSettings
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{LabeledLayout, VerticalLayout}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{CheckBox, ComboBox, Component, Label}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

/**
 * upstream: `autoImportOptionsProvider` built with Kotlin UI DSL; here a child page of "Auto Import"
 */
@ExtensionImpl
final class ScalaAutoImportOptionsProvider extends SimpleConfigurableByProperties with ApplicationConfigurable {

  @RequiredUIAccess
  override protected def createLayout(propertyBuilder: SimpleConfigurableByProperties.PropertyBuilder, uiDisposable: Disposable): Component = {
    val as = ScalaApplicationSettings.getInstance()

    def checkBox(layout: VerticalLayout, text: String, getter: => Boolean, setter: Boolean => Unit): Unit = {
      val box = CheckBox.create(LocalizeValue.of(text))
      propertyBuilder.add(box, () => java.lang.Boolean.valueOf(getter), (value: java.lang.Boolean) => setter(value))
      layout.add(box)
    }

    val root = VerticalLayout.create()

    val insertImports = ComboBox.builder[Integer]()
      .add(Integer.valueOf(CodeInsightSettings.YES), LocalizeValue.of(ApplicationBundle.message("combobox.insert.imports.all")))
      .add(Integer.valueOf(CodeInsightSettings.NO), LocalizeValue.of(ApplicationBundle.message("combobox.insert.imports.none")))
      .add(Integer.valueOf(CodeInsightSettings.ASK), LocalizeValue.of(ApplicationBundle.message("combobox.insert.imports.ask")))
      .build()
    propertyBuilder.add(insertImports, () => Integer.valueOf(as.ADD_IMPORTS_ON_PASTE), (value: Integer) => as.ADD_IMPORTS_ON_PASTE = value.intValue())
    root.add(LabeledBuilder.sided(LocalizeValue.of(JavaBundle.message("combobox.paste.insert.imports")), insertImports))

    val popup = VerticalLayout.create()
    checkBox(popup, ScalaBundle.message("auto.import.show.popup.classes"), as.SHOW_IMPORT_POPUP_CLASSES, as.SHOW_IMPORT_POPUP_CLASSES = _)
    checkBox(popup, ScalaBundle.message("auto.import.show.popup.methods"), as.SHOW_IMPORT_POPUP_STATIC_METHODS, as.SHOW_IMPORT_POPUP_STATIC_METHODS = _)
    checkBox(popup, ScalaBundle.message("auto.import.show.popup.conversions"), as.SHOW_IMPORT_POPUP_CONVERSIONS, as.SHOW_IMPORT_POPUP_CONVERSIONS = _)
    checkBox(popup, ScalaBundle.message("auto.import.show.popup.implicits"), as.SHOW_IMPORT_POPUP_IMPLICITS, as.SHOW_IMPORT_POPUP_IMPLICITS = _)
    checkBox(popup, ScalaBundle.message("auto.import.show.popup.extension.methods"), as.SHOW_IMPORT_POPUP_EXTENSION_METHODS, as.SHOW_IMPORT_POPUP_EXTENSION_METHODS = _)
    root.add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("auto.import.show.import.popup.for")), popup))

    val onTheFly = VerticalLayout.create()
    checkBox(onTheFly, ScalaBundle.message("auto.import.show.popup.classes"), as.ADD_UNAMBIGUOUS_IMPORTS_ON_THE_FLY, as.ADD_UNAMBIGUOUS_IMPORTS_ON_THE_FLY = _)
    checkBox(onTheFly, ScalaBundle.message("auto.import.show.popup.methods"), as.ADD_UNAMBIGUOUS_IMPORTS_ON_THE_FLY_METHODS, as.ADD_UNAMBIGUOUS_IMPORTS_ON_THE_FLY_METHODS = _)
    root.add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("auto.import.add.unambiguous.imports.on.the.fly.for")), onTheFly))

    checkBox(root, ScalaBundle.message("auto.import.optimize.imports.on.the.fly"), as.OPTIMIZE_IMPORTS_ON_THE_FLY, as.OPTIMIZE_IMPORTS_ON_THE_FLY = _)
    // upstream: the comment is a link selecting the "Imports" tab of Scala code style settings
    root.add(Label.create(LocalizeValue.of(ScalaBundle.message("auto.import.find.more.configuration.options"))))

    root
  }

  override def getId: String = "editor.preferences.import.scala"

  override def getParentId: String = "editor.preferences.import"

  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("options.scala.display.name"))
}
