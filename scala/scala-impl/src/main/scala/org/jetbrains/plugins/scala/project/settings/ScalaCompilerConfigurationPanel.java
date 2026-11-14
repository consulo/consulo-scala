package org.jetbrains.plugins.scala.project.settings;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.util.LabeledBuilder;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType;

public class ScalaCompilerConfigurationPanel {

    private final DockLayout myContentPanel;

    private final ScalaCompilerProfilesPanel myProfilesPanel;
    private final ComboBox<IncrementalityType> myIncrementalityType;

    @RequiredUIAccess
    public ScalaCompilerConfigurationPanel(Project project) {
        myIncrementalityType = ComboBox.<IncrementalityType>builder()
                .add(IncrementalityType.SBT, LocalizeValue.of("Zinc"))
                .add(IncrementalityType.IDEA, LocalizeValue.of("IDEA"))
                .build();

        myProfilesPanel = new ScalaCompilerProfilesPanel(project);

        myContentPanel = DockLayout.create();
        myContentPanel.top(LabeledBuilder.simple(LocalizeValue.of(ScalaBundle.message("incrementality.type")), myIncrementalityType));
        myContentPanel.center(myProfilesPanel.getComponent());
    }

    public Component getContentPanel() {
        return myContentPanel;
    }

    public ScalaCompilerProfilesPanel getProfilesPanel() {
        return myProfilesPanel;
    }

    public IncrementalityType getIncrementalityType() {
        return myIncrementalityType.getValue();
    }

    @RequiredUIAccess
    public void setIncrementalityType(IncrementalityType incrementalityType) {
        myIncrementalityType.setValue(incrementalityType);
    }
}
