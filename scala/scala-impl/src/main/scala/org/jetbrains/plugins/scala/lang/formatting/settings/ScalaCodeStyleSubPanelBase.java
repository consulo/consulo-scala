package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorHighlighter;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public abstract class ScalaCodeStyleSubPanelBase extends CodeStyleAbstractPanel {

    protected ScalaCodeStyleSubPanelBase(CodeStyleSettings settings) {
        super(settings);
    }

    @RequiredUIAccess
    public abstract Component getComponent();

    @Override
    @RequiredUIAccess
    public JComponent getPanel() {
        return (JComponent) TargetAWT.to(getComponent());
    }

    @Override
    protected int getRightMargin() {
        return 0;
    }

    @Override
    protected @Nullable EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
        return null;
    }

    @Override
    protected FileType getFileType() {
        return null;
    }

    @Override
    protected @NonNls
    @Nullable String getPreviewText() {
        return null;
    }
}
