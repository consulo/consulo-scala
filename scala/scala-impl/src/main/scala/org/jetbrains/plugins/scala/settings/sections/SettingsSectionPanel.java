package org.jetbrains.plugins.scala.settings.sections;

import consulo.configurable.ConfigurationException;
import consulo.project.Project;

import javax.swing.*;

public abstract class SettingsSectionPanel {
    protected final Project myProject;

    protected SettingsSectionPanel(Project project) {
        myProject = project;
    }

    abstract JComponent getRootPanel();
    abstract boolean isModified();
    abstract void apply() throws ConfigurationException;
    abstract void reset();

    protected static void setValue(JSpinner spinner, int value) {
        spinner.setValue(value);
    }
    protected static int getValue(JSpinner spinner) {
        return (Integer) spinner.getValue();
    }
}
