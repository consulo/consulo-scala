package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.ui.CheckBox;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

import javax.swing.*;
import java.util.List;

public final class CodeGenerationPanel extends ScalaCodeStylePanelBase {

    private final JPanel panel;
    // upstream: com.intellij.application.options.codeStyle.CommenterForm (internal in Consulo)
    private final CheckBox myLineCommentAtFirstColumnCb;
    private final CheckBox myLineCommentAddSpaceCb;
    private final CheckBox myBlockCommentAtFirstColumnCb;

    CodeGenerationPanel(CodeStyleSettings settings) {
        super(settings, ScalaBundle.message("codegeneration.panel.title"));

        myLineCommentAtFirstColumnCb = CheckBox.create(CodeStyleLocalize.checkboxLineCommentAtFirstColumn());
        myLineCommentAddSpaceCb = CheckBox.create(CodeStyleLocalize.checkboxLineCommentAddSpace());
        myBlockCommentAtFirstColumnCb = CheckBox.create(CodeStyleLocalize.checkboxBlockCommentAtFirstColumn());
        myLineCommentAtFirstColumnCb.addClickListener(e -> {
            if (myLineCommentAtFirstColumnCb.getValue()) {
                myLineCommentAddSpaceCb.setValue(false);
            }
            myLineCommentAddSpaceCb.setEnabled(!myLineCommentAtFirstColumnCb.getValue());
        });

        JPanel commenterPanel = new JPanel();
        commenterPanel.setLayout(new BoxLayout(commenterPanel, BoxLayout.Y_AXIS));
        commenterPanel.setBorder(IdeBorderFactory.createTitledBorder(CodeStyleLocalize.titleNamingCommentCode().get()));
        commenterPanel.add(TargetAWT.to(myLineCommentAtFirstColumnCb));
        commenterPanel.add(TargetAWT.to(myLineCommentAddSpaceCb));
        commenterPanel.add(TargetAWT.to(myBlockCommentAtFirstColumnCb));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(IdeBorderFactory.createEmptyBorder(new JBInsets(0, 10, 10, 10)));
        panel.add(commenterPanel);
    }

    // upstream: also BLOCK_COMMENT_ADD_SPACE (no such option in Consulo)
    public static List<String> SupportedCommenterStandardOptionNames = List.of(
            CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name(),
            CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_ADD_SPACE.name(),
            CodeStyleSettingsCustomizable.CommenterOption.BLOCK_COMMENT_AT_FIRST_COLUMN.name()
    );

    @Override
    public void apply(CodeStyleSettings settings) {
        if (isModified(settings)) {
            CommonCodeStyleSettings langSettings = settings.getCommonSettings(ScalaLanguage.INSTANCE);
            langSettings.LINE_COMMENT_AT_FIRST_COLUMN = myLineCommentAtFirstColumnCb.getValue();
            langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = myBlockCommentAtFirstColumnCb.getValue();
            langSettings.LINE_COMMENT_ADD_SPACE = myLineCommentAddSpaceCb.getValue();
        }
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(ScalaLanguage.INSTANCE);
        return myLineCommentAtFirstColumnCb.getValue() != langSettings.LINE_COMMENT_AT_FIRST_COLUMN
                || myBlockCommentAtFirstColumnCb.getValue() != langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN
                || myLineCommentAddSpaceCb.getValue() != langSettings.LINE_COMMENT_ADD_SPACE;
    }

    @Override
    protected JComponent getPanelInner() {
        return panel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(ScalaLanguage.INSTANCE);
        myLineCommentAtFirstColumnCb.setValue(langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myBlockCommentAtFirstColumnCb.setValue(langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setValue(langSettings.LINE_COMMENT_ADD_SPACE && !langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
        myLineCommentAddSpaceCb.setEnabled(!langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
    }
}
