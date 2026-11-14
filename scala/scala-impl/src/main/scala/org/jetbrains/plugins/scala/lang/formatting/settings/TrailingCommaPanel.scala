package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.language.codeStyle.CodeStyleSettings
import consulo.localize.LocalizeValue
import consulo.ui.{CheckBox, ComboBox, Component}
import consulo.ui.layout.{LabeledLayout, TableLayout, VerticalLayout}
import consulo.ui.StaticPosition
import org.jetbrains.plugins.scala.ScalaBundle

import java.lang.reflect.Field
import scala.collection.immutable.ListMap

final class TrailingCommaPanel(settings: CodeStyleSettings) extends ScalaCodeStyleSubPanelBase(settings) {

  import ScalaCodeStyleSettings.TrailingCommaMode

  private val scopeFields: ListMap[String, String] = ListMap(
    ("TRAILING_COMMA_ARG_LIST_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.arguments.list")),
    ("TRAILING_COMMA_PARAMS_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.parameters.list")),
    ("TRAILING_COMMA_TUPLE_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.tuple")),
    ("TRAILING_COMMA_PATTERN_ARG_LIST_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.pattern.arguments.list")),
    ("TRAILING_COMMA_TYPE_PARAMS_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.type.parameters.list")),
    ("TRAILING_COMMA_IMPORT_SELECTOR_ENABLED", ScalaBundle.message("trailing.comma.panel.scope.import.selector")),
  )

  private val trailingCommaModeSelector: ComboBox[TrailingCommaMode] = ComboBox.builder[TrailingCommaMode]()
    .add(TrailingCommaMode.TRAILING_COMMA_KEEP, ScalaBundle.message("trailing.comma.panel.keep"))
    .add(TrailingCommaMode.TRAILING_COMMA_REMOVE_WHEN_MULTILINE, ScalaBundle.message("trailing.comma.panel.remove.when.multiline"))
    .add(TrailingCommaMode.TRAILING_COMMA_ADD_WHEN_MULTILINE, ScalaBundle.message("trailing.comma.panel.add.when.multiline"))
    .build()

  private val scopeCheckboxes: Seq[(CheckBox, Field)] = {
    val styleClass = classOf[ScalaCodeStyleSettings]
    scopeFields.toSeq.map { case (fieldName, title) =>
      (CheckBox.create(LocalizeValue.of(title)), styleClass.getField(fieldName))
    }
  }

  private val innerPanel: Component = {
    val gridRows = 2
    val trailingCommaScopePanel = TableLayout.create(StaticPosition.LEFT)
    scopeCheckboxes.zipWithIndex.foreach { case ((cb, _), idx) =>
      trailingCommaScopePanel.add(cb, TableLayout.cell(idx % gridRows, idx / gridRows))
    }

    trailingCommaModeSelector.addValueListener { _ =>
      val isScopeEnabled = selectedTrailingCommaMode.exists(_ != TrailingCommaMode.TRAILING_COMMA_KEEP)
      scopeCheckboxes.foreach(_._1.setEnabled(isScopeEnabled))
    }

    LabeledLayout.create(
      LocalizeValue.of(ScalaBundle.message("trailing.comma.panel.title")),
      VerticalLayout.create().add(trailingCommaModeSelector).add(trailingCommaScopePanel)
    )
  }

  override def apply(settings: CodeStyleSettings): Unit = {
    if (!isModified(settings)) return
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])

    selectedTrailingCommaMode.foreach { mode =>
      scalaSettings.TRAILING_COMMA_MODE = mode
    }

    scopeCheckboxes.foreach { case (cb, field) =>
      field.set(scalaSettings, cb.getValue)
    }
  }

  override def isModified(settings: CodeStyleSettings): Boolean = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    !selectedTrailingCommaMode.contains(scalaSettings.TRAILING_COMMA_MODE) ||
      scopeCheckboxes.exists { case (cb, field) =>
        field.get(scalaSettings).asInstanceOf[Boolean] != cb.getValue.booleanValue()
      }
  }

  override def resetImpl(settings: CodeStyleSettings): Unit = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])
    trailingCommaModeSelector.setValue(scalaSettings.TRAILING_COMMA_MODE)
    scopeCheckboxes.foreach { case (cb, field) =>
      cb.setValue(field.get(scalaSettings).asInstanceOf[Boolean])
    }
  }

  override def getComponent: Component = innerPanel

  private def selectedTrailingCommaMode: Option[TrailingCommaMode] =
    Option(trailingCommaModeSelector.getValue)
}
