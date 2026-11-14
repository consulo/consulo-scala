package org.jetbrains.plugins.scala.settings.sections;

import consulo.configurable.ConfigurationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.util.ui.ContextHelpLabel;

public abstract class SettingsSectionPanel {
    protected final Project myProject;

    protected SettingsSectionPanel(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    abstract Component getRootPanel();
    @RequiredUIAccess
    abstract boolean isModified();
    @RequiredUIAccess
    abstract void apply() throws ConfigurationException;
    @RequiredUIAccess
    abstract void reset();

    public static LocalizeValue message(String key, Object... params) {
        return LocalizeValue.of(ScalaBundle.message(key, params));
    }

    public static int getValue(IntBox box, int defaultValue) {
        Integer value = box.getValue();
        return value == null ? defaultValue : value;
    }

    @RequiredUIAccess
    public static HorizontalLayout row(Component... components) {
        HorizontalLayout layout = HorizontalLayout.create();
        for (Component component : components) {
            layout.add(component);
        }
        return layout;
    }

    @RequiredUIAccess
    public static HorizontalLayout labeledRow(LocalizeValue label, Component... components) {
        HorizontalLayout layout = HorizontalLayout.create();
        Label labelComponent = Label.create(label);
        if (components.length > 0) {
            labelComponent.setTarget(components[0]);
        }
        layout.add(labelComponent);
        for (Component component : components) {
            layout.add(component);
        }
        return layout;
    }

    @RequiredUIAccess
    public static Component withHelp(Component component, String help) {
        return row(component, ContextHelpLabel.createLabel(help));
    }

    @RequiredUIAccess
    public static <C extends Component> C indented(C component) {
        component.paddingBuilder().leftSet(Space.X_LARGE).apply();
        return component;
    }
}
