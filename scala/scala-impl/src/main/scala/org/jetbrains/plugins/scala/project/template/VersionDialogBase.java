package org.jetbrains.plugins.scala.project.template;

import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.LabeledBuilder;

import javax.swing.*;
import java.util.List;

abstract class VersionDialogBase extends DialogWrapper {
    protected final MutableFlatDataModel<String> myVersions = FlatDataModel.of(List.of());
    protected final ComboBox<String> myVersion;

    @RequiredUIAccess
    VersionDialogBase(JComponent parent) {
        super(parent, false);
        myVersion = ComboBox.create(myVersions);
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        return (JComponent) TargetAWT.to(LabeledBuilder.simple(LocalizeValue.localizeTODO("Version:"), myVersion));
    }
}
