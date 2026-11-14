package org.jetbrains.plugins.scala.settings.sections;

import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.settings.ProblemSolverUtils;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class ProjectViewSettingsSectionPanel extends SettingsSectionPanel {
    private final CheckBox myProjectViewHighlighting;
    private final CheckBox myGroupPackageObjectWithPackage;
    private final VerticalLayout rootPanel;

    @RequiredUIAccess
    protected ProjectViewSettingsSectionPanel(Project project) {
        super(project);

        myGroupPackageObjectWithPackage = CheckBox.create(message("scala.project.settings.form.group.package.object.with.package"));
        myProjectViewHighlighting = CheckBox.create(message("scala.project.settings.form.highlight.nodes.with.errors"));

        rootPanel = VerticalLayout.create();
        rootPanel.add(myGroupPackageObjectWithPackage);
        rootPanel.add(myProjectViewHighlighting);

        reset();
    }

    @Override
    Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.isProjectViewHighlighting() != myProjectViewHighlighting.getValue() ||
                scalaProjectSettings.isGroupPackageObjectWithPackage() != myGroupPackageObjectWithPackage.getValue()
                ;
    }

    @Override
    void apply() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        if (scalaProjectSettings.isProjectViewHighlighting() && !myProjectViewHighlighting.getValue()) {
            ProblemSolverUtils.clearProblemsIn(myProject);
        }
        scalaProjectSettings.setProjectViewHighlighting(myProjectViewHighlighting.getValue());
        if (scalaProjectSettings.isGroupPackageObjectWithPackage() != myGroupPackageObjectWithPackage.getValue()) {
            ProjectViewPane pane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
            if (pane != null) {
                pane.updateFromRoot(true);
            }
        }
        scalaProjectSettings.setGroupPackageObjectWithPackage(myGroupPackageObjectWithPackage.getValue());
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        myProjectViewHighlighting.setValue(scalaProjectSettings.isProjectViewHighlighting());
        myGroupPackageObjectWithPackage.setValue(scalaProjectSettings.isGroupPackageObjectWithPackage());
    }
}
