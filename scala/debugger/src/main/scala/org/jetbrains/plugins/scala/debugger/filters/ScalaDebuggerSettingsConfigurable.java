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

public class ScalaDebuggerSettingsConfigurable extends SimpleConfigurableByProperties implements Configurable {
    private final ScalaDebuggerSettings mySettings;

    public ScalaDebuggerSettingsConfigurable(ScalaDebuggerSettings settings) {
        mySettings = settings;
    }

    @Override
    public String getId() {
        return "scala.debugger.general";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(DebuggerBundle.message("scala.debug.caption"));
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create();

        CheckBox dontShowRuntimeRefs = CheckBox.create(LocalizeValue.of(DebuggerBundle.message("dont.show.runtime.refs")));
        propertyBuilder.add(dontShowRuntimeRefs, () -> mySettings.DONT_SHOW_RUNTIME_REFS, v -> mySettings.DONT_SHOW_RUNTIME_REFS = v);
        root.add(dontShowRuntimeRefs);

        CheckBox friendlyDisplayOfScalaCheckBox = CheckBox.create(LocalizeValue.of(DebuggerBundle.message("friendly.collection.display.enabled")));
        propertyBuilder.add(friendlyDisplayOfScalaCheckBox, () -> mySettings.FRIENDLY_COLLECTION_DISPLAY_ENABLED, v -> mySettings.FRIENDLY_COLLECTION_DISPLAY_ENABLED = v);
        root.add(friendlyDisplayOfScalaCheckBox);

        CheckBox showOuterVariables = CheckBox.create(LocalizeValue.of(DebuggerBundle.message("show.variables.from.outer.scopes.in.variables.view")));
        propertyBuilder.add(showOuterVariables, () -> mySettings.SHOW_VARIABLES_FROM_OUTER_SCOPES, v -> mySettings.SHOW_VARIABLES_FROM_OUTER_SCOPES = v);
        root.add(showOuterVariables);
        return root;
    }
}
