package org.jetbrains.plugins.scala.settings.sections;

import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class WorksheetSettingsSectionPanel extends SettingsSectionPanel {
    private static final int WORKSHEET_RUN_DELAY_MS_MINIMUM = 500;
    private static final int WORKSHEET_RUN_DELAY_MS_MAXIMUM = 5000;
    private static final int WORKSHEET_RUN_DELAY_SPINNER_STEP_SIZE = 10;

    private final IntBox outputSpinner;
    private final IntBox worksheetAutoRunDelaySpinner;
    private final CheckBox runWorksheetInTheCheckBox;
    private final CheckBox useEclipseCompatibilityModeCheckBox;
    private final CheckBox treatScalaScratchFilesCheckBox;
    private final CheckBox collapseWorksheetFoldByCheckBox;
    private final VerticalLayout rootPanel;

    @RequiredUIAccess
    protected WorksheetSettingsSectionPanel(Project project) {
        super(project);

        runWorksheetInTheCheckBox = CheckBox.create(message("scala.project.settings.form.run.worksheet.in.the.compiler.process"));
        useEclipseCompatibilityModeCheckBox = CheckBox.create(message("scala.project.settings.form.use.eclipse.compatibility.mode"));
        treatScalaScratchFilesCheckBox = CheckBox.create(message("scala.project.settings.form.treat.scala.scratch.files.as.worksheet.files"));
        collapseWorksheetFoldByCheckBox = CheckBox.create(message("scala.project.settings.form.collapse.long.output.by.default"));

        outputSpinner = IntBox.create(1).withRange(1, Integer.MAX_VALUE);
        worksheetAutoRunDelaySpinner = IntBox.create(WORKSHEET_RUN_DELAY_MS_MINIMUM)
                .withRange(WORKSHEET_RUN_DELAY_MS_MINIMUM, WORKSHEET_RUN_DELAY_MS_MAXIMUM)
                .withStep(WORKSHEET_RUN_DELAY_SPINNER_STEP_SIZE);

        rootPanel = VerticalLayout.create();
        rootPanel.add(runWorksheetInTheCheckBox);
        rootPanel.add(useEclipseCompatibilityModeCheckBox);
        rootPanel.add(treatScalaScratchFilesCheckBox);
        rootPanel.add(collapseWorksheetFoldByCheckBox);
        rootPanel.add(labeledRow(message("scala.project.settings.form.output.cutoff.limit"),
                outputSpinner, Label.create(message("scala.project.settings.form.output.cutoff.limit.units"))));
        rootPanel.add(labeledRow(message("scala.project.settings.form.delay.before.auto.run"),
                worksheetAutoRunDelaySpinner, Label.create(message("scala.project.settings.form.delay.before.auto.run.units"))));

        reset();
    }

    @Override
    Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.getOutputLimit() != getValue(outputSpinner, 1) ||
                scalaProjectSettings.isInProcessMode() != runWorksheetInTheCheckBox.getValue() ||
                scalaProjectSettings.isWorksheetFoldCollapsedByDefault() != collapseWorksheetFoldByCheckBox.getValue() ||
                scalaProjectSettings.isUseEclipseCompatibility() != useEclipseCompatibilityModeCheckBox.getValue() ||
                scalaProjectSettings.isTreatScratchFilesAsWorksheet() != treatScalaScratchFilesCheckBox.getValue() ||
                scalaProjectSettings.getAutoRunDelay() != getValue(worksheetAutoRunDelaySpinner, WORKSHEET_RUN_DELAY_MS_MINIMUM)
                ;
    }

    @Override
    void apply() {
        if (!isModified()) return;

        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setOutputLimit(getValue(outputSpinner, 1));
        scalaProjectSettings.setInProcessMode(runWorksheetInTheCheckBox.getValue());
        scalaProjectSettings.setWorksheetFoldCollapsedByDefault(collapseWorksheetFoldByCheckBox.getValue());
        scalaProjectSettings.setUseEclipseCompatibility(useEclipseCompatibilityModeCheckBox.getValue());
        scalaProjectSettings.setTreatScratchFilesAsWorksheet(treatScalaScratchFilesCheckBox.getValue());

        scalaProjectSettings.setAutoRunDelay(getValue(worksheetAutoRunDelaySpinner, WORKSHEET_RUN_DELAY_MS_MINIMUM));
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        outputSpinner.setValue(scalaProjectSettings.getOutputLimit());
        runWorksheetInTheCheckBox.setValue(scalaProjectSettings.isInProcessMode());
        collapseWorksheetFoldByCheckBox.setValue(scalaProjectSettings.isWorksheetFoldCollapsedByDefault());
        useEclipseCompatibilityModeCheckBox.setValue(scalaProjectSettings.isUseEclipseCompatibility());
        treatScalaScratchFilesCheckBox.setValue(scalaProjectSettings.isTreatScratchFilesAsWorksheet());

        worksheetAutoRunDelaySpinner.setValue(scalaProjectSettings.getAutoRunDelay());
    }
}
