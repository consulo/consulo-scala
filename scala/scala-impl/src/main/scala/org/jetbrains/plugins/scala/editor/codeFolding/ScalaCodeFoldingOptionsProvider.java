package org.jetbrains.plugins.scala.editor.codeFolding;

import consulo.annotation.component.ExtensionImpl;

import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.editor.ScalaEditorBundle;
import org.jetbrains.plugins.scala.settings.ScalaCodeFoldingSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

// upstream: BeanConfigurable implementing com.intellij.application.options.editor.CodeFoldingOptionsProvider,
// in Consulo code folding options are child configurables of "editor.preferences.folding"
@ExtensionImpl
public class ScalaCodeFoldingOptionsProvider extends SimpleConfigurableByProperties implements ApplicationConfigurable {

  @Override
  public String getId() {
    return "editor.preferences.folding.scala";
  }

  @Override
  public String getParentId() {
    return "editor.preferences.folding";
  }

  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.of("Scala");
  }

  @RequiredUIAccess
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
    VerticalLayout layout = VerticalLayout.create();
    ScalaCodeFoldingSettings s = ScalaCodeFoldingSettings.getInstance();

    checkBox(ScalaEditorBundle.message("checkbox.collapse.block.comments"), layout, propertyBuilder, s::isCollapseBlockComments, s::setCollapseBlockComments);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.method.call.bodies"), layout, propertyBuilder, s::isCollapseMethodCallBodies, s::setCollapseMethodCallBodies);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.template.bodies"), layout, propertyBuilder, s::isCollapseTemplateBodies, s::setCollapseTemplateBodies);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.other.definition.bodies"), layout, propertyBuilder, s::isCollapseDefinitionWithAssignmentBodies, s::setCollapseDefinitionWithAssignmentBodies);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.type.lambdas"), layout, propertyBuilder, s::isCollapseTypeLambdas, s::setCollapseTypeLambdas);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.packages"), layout, propertyBuilder, s::isCollapsePackagings, s::setCollapsePackagings);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.multiline.strings"), layout, propertyBuilder, s::isCollapseMultilineStrings, s::setCollapseMultilineStrings);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.custom.regions"), layout, propertyBuilder, s::isCollapseCustomRegions, s::setCollapseCustomRegions);
    checkBox(ScalaEditorBundle.message("checkbox.collapse.multiline.blocks"), layout, propertyBuilder, s::isCollapseMultilineBlocks, s::setCollapseMultilineBlocks);
    checkBox(ScalaEditorBundle.message("checkbox.add.folding.for.all.blocks"), layout, propertyBuilder, s::isFoldingForAllBlocks, s::setFoldingForAllBlocks);
    return layout;
  }

  @RequiredUIAccess
  private static void checkBox(String text,
                               VerticalLayout layout,
                               PropertyBuilder builder,
                               Supplier<Boolean> getter,
                               Consumer<Boolean> setter) {
    CheckBox checkBox = CheckBox.create(LocalizeValue.of(text));
    builder.add(checkBox, getter, setter);
    layout.add(checkBox);
  }
}
