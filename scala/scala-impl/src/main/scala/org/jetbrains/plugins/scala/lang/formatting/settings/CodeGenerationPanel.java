package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.api.ScalaLanguage;

import java.util.List;

public final class CodeGenerationPanel extends ScalaCodeStylePanelBase {

    private final VerticalLayout panel;
    // upstream: com.intellij.application.options.codeStyle.CommenterForm (internal in Consulo)
    private final CheckBox myLineCommentAtFirstColumnCb;
    private final CheckBox myLineCommentAddSpaceCb;
    private final CheckBox myBlockCommentAtFirstColumnCb;

    @RequiredUIAccess
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

        VerticalLayout commenterPanel = VerticalLayout.create()
                .add(myLineCommentAtFirstColumnCb)
                .add(myLineCommentAddSpaceCb)
                .add(myBlockCommentAtFirstColumnCb);

        panel = VerticalLayout.create();
        panel.add(LabeledLayout.create(CodeStyleLocalize.titleNamingCommentCode(), commenterPanel));
        panel.paddingBuilder().horizontalSet(Space.MEDIUM).bottomSet(Space.MEDIUM).apply();
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
    protected Component getPanelInner() {
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
