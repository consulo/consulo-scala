package org.jetbrains.plugins.scala.conversion.copy;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.conversion.ScalaConversionBundle;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import javax.swing.*;

public class ScalaPasteFromJavaDialog extends DialogWrapper {
    private final VerticalLayout myPanel;
    private final CheckBox donTShowThisCheckBox;
    private final ScalaProjectSettings mySettings;

    @RequiredUIAccess
    private ScalaPasteFromJavaDialog(Project project,
                                     ScalaProjectSettings settings,
                                     CopyFrom copyFrom) {
        super(project, true);
        mySettings = settings;
        setModal(true);
        setTitle(ScalaConversionBundle.message("scala.convert.code.from.java"));
        final String description = copyFrom == CopyFrom.JavaFile
                ? ScalaConversionBundle.message("scala.convert.code.from.java.file.description")
                : ScalaConversionBundle.message("scala.convert.code.from.java.text.description");
        myPanel = VerticalLayout.create();
        myPanel.add(Label.create(LocalizeValue.of(description)));
        donTShowThisCheckBox = CheckBox.create(LocalizeValue.of(ScalaConversionBundle.message("scala.convert.code.from.java.dont.show.this.dialog.next.time")));
        myPanel.add(donTShowThisCheckBox);
        init();
    }

    public enum CopyFrom {JavaFile, Text}

    /**
     * @return true if we should continue the conversion from Java to Scala
     */
    public static boolean showAndGet(CopyFrom copyFrom, Project project) {
        ScalaProjectSettings projectSettings = ScalaProjectSettings.getInstance(project);
        return projectSettings.isDontShowConversionDialog() ||
                new ScalaPasteFromJavaDialog(project, projectSettings, copyFrom).showAndGet();
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        return (JComponent) TargetAWT.to(myPanel);
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    @RequiredUIAccess
    protected void doOKAction() {
        if (donTShowThisCheckBox.getValueOrError()) {
            mySettings.setDontShowConversionDialog(true);
        }
        super.doOKAction();
    }
}
