package org.jetbrains.plugins.scala.lang.formatting.settings;

import com.intellij.application.options.codeStyle.CommenterForm;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.util.collection.ContainerUtil;
import com.intellij.util.containers.ImmutableList;
import consulo.ui.ex.awt.JBInsets;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

import javax.swing.*;
import java.util.List;

public final class CodeGenerationPanel extends ScalaCodeStylePanelBase {

    private final JPanel panel;
    private final CommenterForm myCommenterForm;

    CodeGenerationPanel(CodeStyleSettings settings) {
        super(settings, ScalaBundle.message("codegeneration.panel.title"));

        myCommenterForm = new CommenterForm(ScalaLanguage.INSTANCE);
        myCommenterForm.showStandardOptions(SupportedCommenterStandardOptionNames.toArray(new String[]{}));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(IdeBorderFactory.createEmptyBorder(new JBInsets(0, 10, 10, 10)));
        panel.add(myCommenterForm.getCommenterPanel());
    }

    public static List<String> SupportedCommenterStandardOptionNames = List.of(
            CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name(),
            CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_ADD_SPACE.name(),
            CodeStyleSettingsCustomizable.CommenterOption.BLOCK_COMMENT_AT_FIRST_COLUMN.name(),
            CodeStyleSettingsCustomizable.CommenterOption.BLOCK_COMMENT_ADD_SPACE.name()
    );

    @Override
    public void apply(CodeStyleSettings settings) {
        if (isModified(settings)) myCommenterForm.apply(settings);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        return myCommenterForm.isModified(settings);
    }

    @Override
    protected JComponent getPanelInner() {
        return panel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        myCommenterForm.reset(settings);
    }
}
