package org.jetbrains.plugins.scala.debugger.filters;

import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.debugger.DebuggerBundle;

public class ScalaSteppingConfigurable extends SimpleConfigurableByProperties implements Configurable {
    private final ScalaDebuggerSettings mySettings;

    public ScalaSteppingConfigurable(ScalaDebuggerSettings settings) {
        mySettings = settings;
    }

    @Override
    public String getId() {
        return "scala.debugger.stepping";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(DebuggerBundle.message("scala.debug.caption"));
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create();

        CheckBox ignoreScalaMethods = CheckBox.create(LocalizeValue.of(DebuggerBundle.message("scala.debug.disable.specific.methods")));
        propertyBuilder.add(ignoreScalaMethods, () -> mySettings.DEBUG_DISABLE_SPECIFIC_SCALA_METHODS, v -> mySettings.DEBUG_DISABLE_SPECIFIC_SCALA_METHODS = v);
        root.add(ignoreScalaMethods);
        return root;
    }
}
