package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorHighlighter;
import consulo.virtualFileSystem.fileType.FileType;
import com.intellij.openapi.fileTypes.FileTypeEditorHighlighterProviders;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.ui.ex.awt.ScrollPaneFactory;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.api.ScalaFileType;

import javax.swing.*;

abstract public class ScalaCodeStylePanelBase extends CodeStyleAbstractPanel {

    @Nullable
    private final String myTabTitle;
    private JComponent myPanel;

    protected ScalaCodeStylePanelBase(CodeStyleSettings settings, String tabTitle) {
        super(settings);
        myTabTitle = tabTitle;
    }

    protected ScalaCodeStylePanelBase(CodeStyleSettings settings) {
        super(settings);
        myTabTitle = null;
    }

    @Override
    protected final String getTabTitle() {
        return myTabTitle != null ? myTabTitle : super.getTabTitle();
    }

    @Override
    protected final EditorHighlighter createHighlighter(EditorColorsScheme colors) {
        FileType fileType = getFileType();
        return FileTypeEditorHighlighterProviders.getInstance()
                .forFileType(fileType)
                .getEditorHighlighter(null, fileType, null, colors);
    }

    @Override
    protected final FileType getFileType() {
        return ScalaFileType.INSTANCE;
    }

    @Nullable
    @Override
    protected final String getPreviewText() {
        return null;
    }

    @Override
    protected final int getRightMargin() {
        return 0;
    }

    @Override
    public final JComponent getPanel() {
        if (myPanel == null) {
            JComponent contentPanel = getPanelInner();
            myPanel = ScrollPaneFactory.createScrollPane(contentPanel, true);
        }
        return myPanel;
    }

    abstract protected JComponent getPanelInner();
}
