package org.jetbrains.plugins.scala.settings

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.{ApplicationConfigurable, SimpleConfigurableByProperties}
import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{LabeledLayout, VerticalLayout}
import consulo.ui.{CheckBox, Component}
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * upstream: `editorSmartKeysConfigurable` built with Kotlin UI DSL ("Scala" child page of Smart Keys)
 */
@ExtensionImpl
final class ScalaEditorSmartKeysConfigurable extends SimpleConfigurableByProperties with ApplicationConfigurable {

  @RequiredUIAccess
  override protected def createLayout(propertyBuilder: SimpleConfigurableByProperties.PropertyBuilder, uiDisposable: Disposable): Component = {
    val settings = ScalaApplicationSettings.getInstance

    def checkBox(layout: VerticalLayout, text: String, tooltip: Option[String], getter: => Boolean, setter: Boolean => Unit): Unit = {
      val box = CheckBox.create(LocalizeValue.of(text))
      tooltip.foreach(t => box.setToolTipText(LocalizeValue.of(t)))
      propertyBuilder.add(box, () => java.lang.Boolean.valueOf(getter), (value: java.lang.Boolean) => setter(value))
      layout.add(box)
    }

    val root = VerticalLayout.create()
    checkBox(root, ScalaBundle.message("indent.pasted.lines.at.caret"), None,
      settings.INDENT_PASTED_LINES_AT_CARET, settings.INDENT_PASTED_LINES_AT_CARET = _)
    checkBox(root, ScalaBundle.message("insert.pair.multiline.quotes"), None,
      settings.INSERT_MULTILINE_QUOTES, settings.INSERT_MULTILINE_QUOTES = _)
    checkBox(root, ScalaBundle.message("upgrade.to.interpolated"), None,
      settings.UPGRADE_TO_INTERPOLATED, settings.UPGRADE_TO_INTERPOLATED = _)
    checkBox(root, ScalaBundle.message("wrap.single.expression.body"), None,
      settings.WRAP_SINGLE_EXPRESSION_BODY, settings.WRAP_SINGLE_EXPRESSION_BODY = _)
    checkBox(root, ScalaBundle.message("delete.closing.brace"), None,
      settings.DELETE_CLOSING_BRACE, settings.DELETE_CLOSING_BRACE = _)

    val bracesGroup = VerticalLayout.create()
    checkBox(bracesGroup,
      ScalaBundle.message("insert.block.braces.automatically.based.on.indentation"),
      Some(ScalaBundle.message("insert.block.braces.automatically.based.on.indentation.tooltip")),
      settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY, settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY = _)
    checkBox(bracesGroup,
      ScalaBundle.message("remove.block.braces.automatically.based.on.indentation"),
      Some(ScalaBundle.message("remove.block.braces.automatically.based.on.indentation.tooltip")),
      settings.HANDLE_BLOCK_BRACES_REMOVAL_AUTOMATICALLY, settings.HANDLE_BLOCK_BRACES_REMOVAL_AUTOMATICALLY = _)
    root.add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("control.curly.braces.based.on.line.indents")), bracesGroup))

    root
  }

  override def getId: String = "editor.preferences.smartKeys.scala"

  override def getParentId: String = "editor.preferences.smartKeys"

  //noinspection ScalaExtractStringToBundle
  override def getDisplayName: LocalizeValue = LocalizeValue.of("Scala")
}
