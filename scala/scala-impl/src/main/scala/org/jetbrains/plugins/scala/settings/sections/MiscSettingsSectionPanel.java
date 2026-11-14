package org.jetbrains.plugins.scala.settings.sections;

import consulo.util.lang.StringUtil;

import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;
import org.jetbrains.plugins.scala.settings.uiControls.DependencyAwareInjectionSettings;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;
import static org.jetbrains.plugins.scala.settings.uiControls.DependencyAwareInjectionSettings.EP_NAME;

public class MiscSettingsSectionPanel extends SettingsSectionPanel {
    private final TextBox scalaTestDefaultSuperClass;
    private final ComboBox<ScalaProjectSettings.TrailingCommasMode> trailingCommasComboBox;
    private final VerticalLayout rootPanel;

    private final List<DependencyAwareInjectionSettings.ComponentWithSettings> extraSettings = new ArrayList<>();

    @RequiredUIAccess
    public MiscSettingsSectionPanel(Project project) {
        super(project);

        scalaTestDefaultSuperClass = TextBox.create().withVisibleLength(25);
        trailingCommasComboBox = ComboBox.<ScalaProjectSettings.TrailingCommasMode>builder()
                .add(ScalaProjectSettings.TrailingCommasMode.Enabled, message("trailing.commas.mode.enabled"))
                .add(ScalaProjectSettings.TrailingCommasMode.Disabled, message("trailing.commas.mode.disabled"))
                .add(ScalaProjectSettings.TrailingCommasMode.Auto, message("trailing.commas.mode.auto"))
                .build();

        // DependencyAwareInjectionSettings contributors still build Swing UI
        JPanel injectionJPanel = new JPanel(new GridLayout(1, 1));
        injectionJPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        for (DependencyAwareInjectionSettings uiWithDependency : EP_NAME.getExtensionList()) {
            extraSettings.add(uiWithDependency.createComponent(injectionJPanel));
        }

        rootPanel = VerticalLayout.create();
        rootPanel.add(labeledRow(message("scala.project.settings.form.scalatest.default.super.class"), scalaTestDefaultSuperClass));
        rootPanel.add(labeledRow(message("scala.project.settings.form.trailing.commas"), trailingCommasComboBox));
        rootPanel.add(TargetAWT.wrap(injectionJPanel));

        reset();
    }

    @Override
    public Component getRootPanel() {
        return rootPanel;
    }

    @Override
    public boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        for (DependencyAwareInjectionSettings.ComponentWithSettings setting : extraSettings) {
            if (setting.isModified(scalaProjectSettings)) return true;
        }

        return !scalaProjectSettings.getScalaTestDefaultSuperClass().equals(StringUtil.notNullize(scalaTestDefaultSuperClass.getValue())) ||
                scalaProjectSettings.getTrailingCommasMode() != trailingCommasComboBox.getValue();
    }

    @Override
    public void apply() throws ConfigurationException {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setScalaTestDefaultSuperClass(StringUtil.notNullize(scalaTestDefaultSuperClass.getValue()));

        ScalaProjectSettings.TrailingCommasMode trailingComa = trailingCommasComboBox.getValue();
        if (trailingComa != null)
            scalaProjectSettings.setTrailingCommasMode(trailingComa);

        extraSettings.forEach(s -> s.saveSettings(scalaProjectSettings));
    }

    @Override
    public void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaTestDefaultSuperClass.setValue(scalaProjectSettings.getScalaTestDefaultSuperClass());
        trailingCommasComboBox.setValue(scalaProjectSettings.getTrailingCommasMode());

        for (DependencyAwareInjectionSettings.ComponentWithSettings setting : extraSettings) {
            setting.loadSettings(scalaProjectSettings);
        }
    }
}
