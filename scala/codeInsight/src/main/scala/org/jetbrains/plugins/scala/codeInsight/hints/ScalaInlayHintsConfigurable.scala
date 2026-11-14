package org.jetbrains.plugins.scala
package codeInsight
package hints

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.{ApplicationConfigurable, ConfigurationException, SimpleConfigurableByProperties}
import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{HorizontalLayout, LabeledLayout, VerticalLayout}
import consulo.ui.{CheckBox, Component, IntBox, Label}
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

/**
 * upstream: ScalaInlayHintsSettingsProvider with the settings models of type, parameter name, method chain and range hints
 * on the platform "Inlay Hints" page (InlaySettingsProvider is internal in Consulo); no previews, no parameter exclude list
 */
@ExtensionImpl
final class ScalaInlayHintsConfigurable extends SimpleConfigurableByProperties with ApplicationConfigurable {

  @RequiredUIAccess
  override protected def createLayout(propertyBuilder: SimpleConfigurableByProperties.PropertyBuilder, uiDisposable: Disposable): Component = {
    val settings = ScalaCodeInsightSettings.getInstance

    def checkBox(layout: VerticalLayout, text: String, getter: => Boolean, setter: Boolean => Unit): Unit = {
      val box = CheckBox.create(LocalizeValue.of(text))
      propertyBuilder.add(box, () => java.lang.Boolean.valueOf(getter), (value: java.lang.Boolean) => setter(value))
      layout.add(box)
    }

    def intBox(layout: VerticalLayout, label: String, suffix: Option[String], min: Int, max: Int,
               getter: => Int, setter: Int => Unit): Unit = {
      val box = IntBox.create()
      box.setRange(min, max)
      propertyBuilder.add(box, () => Integer.valueOf(getter), (value: Integer) => setter(value))
      val line = HorizontalLayout.create()
      line.add(Label.create(LocalizeValue.of(label)))
      line.add(box)
      suffix.foreach(s => line.add(Label.create(LocalizeValue.of(s))))
      layout.add(line)
    }

    def group(root: VerticalLayout, title: LocalizeValue)(fill: VerticalLayout => Unit): Unit = {
      val layout = VerticalLayout.create()
      fill(layout)
      root.add(LabeledLayout.create(title, layout))
    }

    val root = VerticalLayout.create()

    //noinspection ScalaExtractStringToBundle
    group(root, LocalizeValue.localizeTODO("Type hints")) { layout =>
      checkBox(layout, ScalaCodeInsightBundle.message("member.variables"), settings.showPropertyType, settings.showPropertyType = _)
      checkBox(layout, ScalaCodeInsightBundle.message("local.variables"), settings.showLocalVariableType, settings.showLocalVariableType = _)
      checkBox(layout, ScalaCodeInsightBundle.message("method.results"), settings.showFunctionReturnType, settings.showFunctionReturnType = _)
      checkBox(layout, ScalaCodeInsightBundle.message("show.types.even.if.they.are.obvious"), settings.showObviousType, settings.showObviousType = _)
      checkBox(layout, ScalaCodeInsightBundle.message("inlay.hints.respect.vertical.alignment"), settings.preserveIndents, settings.preserveIndents = _)
      intBox(layout,
        ScalaCodeInsightBundle.message("inlay.hints.limit.hint.length.to"), Some(ScalaCodeInsightBundle.message("inlay.hints.characters")),
        ScalaCodeInsightSettings.MIN_PRESENTATION_LENGTH, ScalaCodeInsightSettings.MAX_PRESENTATION_LENGTH,
        settings.presentationLength, settings.presentationLength = _)
    }

    //noinspection ScalaExtractStringToBundle
    group(root, LocalizeValue.localizeTODO("Parameter name hints")) { layout =>
      checkBox(layout, ScalaCodeInsightBundle.message("parameter.name.hints.action.description"), settings.showParameterNames, settings.showParameterNames = _)
    }

    group(root, LocalizeValue.of(ScalaCodeInsightBundle.message("method.chain.hints"))) { layout =>
      checkBox(layout, ScalaCodeInsightBundle.message("method.chain.hints.action.description"), settings.showMethodChainInlayHints, settings.showMethodChainInlayHints = _)
      checkBox(layout, ScalaCodeInsightBundle.message("in.a.separate.column"), settings.alignMethodChainInlayHints, settings.alignMethodChainInlayHints = _)
      intBox(layout, ScalaCodeInsightBundle.message("inlay.hints.only.when.there.are.at.least"), None, 1, 99,
        settings.uniqueTypesToShowMethodChains, settings.uniqueTypesToShowMethodChains = _)
    }

    //noinspection ScalaExtractStringToBundle
    group(root, LocalizeValue.localizeTODO("Range hints")) { layout =>
      checkBox(layout, ScalaCodeInsightBundle.message("show.range.hints.for.to.and.until"), settings.showRangeHintsForToAndUntil, settings.showRangeHintsForToAndUntil = _)
      checkBox(layout, ScalaCodeInsightBundle.message("show.exclusive.range.hint"), settings.showExclusiveRangeHint, settings.showExclusiveRangeHint = _)
    }

    root
  }

  @RequiredUIAccess
  @throws[ConfigurationException]
  override protected def apply(component: SimpleConfigurableByProperties.LayoutWrapper): Unit = {
    super.apply(component)
    ImplicitHints.updateInAllEditors()
  }

  override def getId: String = ScalaInlayHintsConfigurable.Id

  override def getParentId: String = "inlay.hints"

  //noinspection ScalaExtractStringToBundle
  override def getDisplayName: LocalizeValue = LocalizeValue.of("Scala")
}

object ScalaInlayHintsConfigurable {
  final val Id = "inlay.hints.scala"
}
