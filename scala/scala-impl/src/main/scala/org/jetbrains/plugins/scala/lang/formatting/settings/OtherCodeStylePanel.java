package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.util.lang.StringUtil;

import consulo.disposer.Disposer;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.ScalaBundle;

// TODO: maybe group settings "Replace ... with unicode symbol"?
//  we currently have 4 similar settings in "Other" tab
public final class OtherCodeStylePanel extends ScalaCodeStylePanelBase {

    private final CheckBox enforceFunctionalSyntaxForCheckBox;
    private final VerticalLayout contentPanel;
    private final CheckBox replaceWithUnicodeSymbolCheckBox;
    private final CheckBox replaceWithUnicodeSymbolCheckBox1;
    private final CheckBox replaceInForGeneratorCheckBox;
    private final CheckBox replaceLambdaWithGreekLetter;
    private final CheckBox alternateIndentationForParamsCheckBox;
    private final IntBox alternateIndentationForParamsSpinner;
    private final Label spacesLabel;
    private final CheckBox reformatOnCompileCheckBox;
    private final TrailingCommaPanel trailingCommaPanel = new TrailingCommaPanel(getSettings());
    private final TextBox implicitValueClassPrefix;
    private final TextBox implicitValueClassSuffix;

    private final Scala3SettingsPanel scala3SettingsPanel = new Scala3SettingsPanel(getSettings());

    @RequiredUIAccess
    OtherCodeStylePanel(CodeStyleSettings settings) {
        super(settings, ScalaBundle.message("other.panel.title"));

        enforceFunctionalSyntaxForCheckBox = checkBox("other.panel.enforce.functional.syntax.for.methods.with.unit.return.type");
        reformatOnCompileCheckBox = checkBox("other.panel.reformat.on.compile");
        replaceWithUnicodeSymbolCheckBox = checkBox("other.panel.replace.with.unicode.symbol");
        replaceWithUnicodeSymbolCheckBox1 = checkBox("other.panel.replace.with.unicode.symbol1");
        replaceInForGeneratorCheckBox = checkBox("other.panel.replace.in.for.generator.with.unicode.symbol");
        replaceLambdaWithGreekLetter = checkBox("other.panel.kind.projector.replace.lambda.with.unicode.symbol");
        alternateIndentationForParamsCheckBox = checkBox("other.panel.alternate.indentation.for.constructor.args.and.parameter.declarations");
        alternateIndentationForParamsSpinner = IntBox.create(4).withRange(1, Integer.MAX_VALUE);
        spacesLabel = Label.create(LocalizeValue.of(ScalaBundle.message("other.panel.spaces")));
        implicitValueClassPrefix = TextBox.create();
        implicitValueClassSuffix = TextBox.create("Ops");

        contentPanel = VerticalLayout.create();
        contentPanel.add(enforceFunctionalSyntaxForCheckBox);
        contentPanel.add(reformatOnCompileCheckBox);
        contentPanel.add(replaceWithUnicodeSymbolCheckBox);
        contentPanel.add(replaceWithUnicodeSymbolCheckBox1);
        contentPanel.add(replaceInForGeneratorCheckBox);
        contentPanel.add(replaceLambdaWithGreekLetter);
        contentPanel.add(HorizontalLayout.create()
                .add(alternateIndentationForParamsCheckBox)
                .add(alternateIndentationForParamsSpinner)
                .add(spacesLabel));
        contentPanel.add(scala3SettingsPanel.getComponent());
        contentPanel.add(trailingCommaPanel.getComponent());
        contentPanel.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.of(ScalaBundle.message("other.panel.implicit.class.prefix.suffix"))))
                .add(implicitValueClassPrefix)
                .add(implicitValueClassSuffix));
        contentPanel.paddingBuilder().allSet(Space.MEDIUM).apply();

        resetImpl(settings);
    }

    @RequiredUIAccess
    private static CheckBox checkBox(String key) {
        return CheckBox.create(LocalizeValue.of(ScalaBundle.message(key)));
    }

    @Override
    public void dispose() {
        super.dispose();
        Disposer.dispose(trailingCommaPanel);
        Disposer.dispose(scala3SettingsPanel);
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        if (!isModified(settings)) return;

        ScalaCodeStyleSettings scalaCodeStyleSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);
        scalaCodeStyleSettings.ENFORCE_FUNCTIONAL_SYNTAX_FOR_UNIT = enforceFunctionalSyntaxForCheckBox.getValue();
        scalaCodeStyleSettings.REPLACE_CASE_ARROW_WITH_UNICODE_CHAR = replaceWithUnicodeSymbolCheckBox.getValue();
        scalaCodeStyleSettings.REPLACE_MAP_ARROW_WITH_UNICODE_CHAR = replaceWithUnicodeSymbolCheckBox1.getValue();
        scalaCodeStyleSettings.REPLACE_FOR_GENERATOR_ARROW_WITH_UNICODE_CHAR = replaceInForGeneratorCheckBox.getValue();
        scalaCodeStyleSettings.REPLACE_LAMBDA_WITH_GREEK_LETTER = replaceLambdaWithGreekLetter.getValue();
        scalaCodeStyleSettings.USE_ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS = alternateIndentationForParamsCheckBox.getValue();
        scalaCodeStyleSettings.ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS = alternateIndentationForParamsSpinner.getValue();
        scalaCodeStyleSettings.REFORMAT_ON_COMPILE = reformatOnCompileCheckBox.getValue();
        scalaCodeStyleSettings.IMPLICIT_VALUE_CLASS_PREFIX = StringUtil.notNullize(implicitValueClassPrefix.getValue());
        scalaCodeStyleSettings.IMPLICIT_VALUE_CLASS_SUFFIX = StringUtil.notNullize(implicitValueClassSuffix.getValue());
        trailingCommaPanel.apply(settings);
        scala3SettingsPanel.apply(settings);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean isModified(CodeStyleSettings settings) {
        ScalaCodeStyleSettings ss = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        if (ss.ENFORCE_FUNCTIONAL_SYNTAX_FOR_UNIT != enforceFunctionalSyntaxForCheckBox.getValue()) return true;
        if (ss.REPLACE_CASE_ARROW_WITH_UNICODE_CHAR != replaceWithUnicodeSymbolCheckBox.getValue()) return true;
        if (ss.REPLACE_MAP_ARROW_WITH_UNICODE_CHAR != replaceWithUnicodeSymbolCheckBox1.getValue()) return true;
        if (ss.REPLACE_FOR_GENERATOR_ARROW_WITH_UNICODE_CHAR != replaceInForGeneratorCheckBox.getValue()) return true;
        if (ss.REPLACE_LAMBDA_WITH_GREEK_LETTER != replaceLambdaWithGreekLetter.getValue()) return true;
        if (ss.USE_ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS != alternateIndentationForParamsCheckBox.getValue())
            return true;
        if (ss.ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS != alternateIndentationForParamsSpinner.getValue())
            return true;
        if (ss.REFORMAT_ON_COMPILE != reformatOnCompileCheckBox.getValue()) return true;
        if (!ss.IMPLICIT_VALUE_CLASS_PREFIX.equals(StringUtil.notNullize(implicitValueClassPrefix.getValue()))) return true;
        if (!ss.IMPLICIT_VALUE_CLASS_SUFFIX.equals(StringUtil.notNullize(implicitValueClassSuffix.getValue()))) return true;
        if (trailingCommaPanel.isModified(settings)) return true;
        if (scala3SettingsPanel.isModified(settings)) return true;

        return false;
    }

    @Override
    protected Component getPanelInner() {
        return contentPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        ScalaCodeStyleSettings ss = settings.getCustomSettings(ScalaCodeStyleSettings.class);
        enforceFunctionalSyntaxForCheckBox.setValue(ss.ENFORCE_FUNCTIONAL_SYNTAX_FOR_UNIT);
        replaceWithUnicodeSymbolCheckBox.setValue(ss.REPLACE_CASE_ARROW_WITH_UNICODE_CHAR);
        replaceWithUnicodeSymbolCheckBox1.setValue(ss.REPLACE_MAP_ARROW_WITH_UNICODE_CHAR);
        replaceInForGeneratorCheckBox.setValue(ss.REPLACE_FOR_GENERATOR_ARROW_WITH_UNICODE_CHAR);
        replaceLambdaWithGreekLetter.setValue(ss.REPLACE_LAMBDA_WITH_GREEK_LETTER);
        alternateIndentationForParamsCheckBox.setValue(ss.USE_ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS);
        alternateIndentationForParamsSpinner.setValue(ss.ALTERNATE_CONTINUATION_INDENT_FOR_PARAMS);
        reformatOnCompileCheckBox.setValue(ss.REFORMAT_ON_COMPILE);
        implicitValueClassPrefix.setValue(ss.IMPLICIT_VALUE_CLASS_PREFIX);
        implicitValueClassSuffix.setValue(ss.IMPLICIT_VALUE_CLASS_SUFFIX);
        trailingCommaPanel.resetImpl(settings);
        scala3SettingsPanel.resetImpl(settings);
    }

    public void toggleExternalFormatter(boolean useExternalFormatter) {
        alternateIndentationForParamsCheckBox.setVisible(!useExternalFormatter);
        alternateIndentationForParamsSpinner.setVisible(!useExternalFormatter);
        spacesLabel.setVisible(!useExternalFormatter);
    }
}
