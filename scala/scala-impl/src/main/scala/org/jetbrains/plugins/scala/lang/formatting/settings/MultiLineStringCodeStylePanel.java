package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.util.lang.StringUtil;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import org.jetbrains.plugins.scala.ScalaBundle;

public final class MultiLineStringCodeStylePanel extends ScalaCodeStylePanelBase {

    private final VerticalLayout panel1;
    private final CheckBox openingQuotesOnNewCheckBox;
    private final CheckBox closingQuotesOnNewLine;
    private final CheckBox insertMarginChar;
    private final CheckBox processMarginCheckBox;
    private final TextBox marginCharTextField;
    private final IntBox marginIndentSpinner;
    private final CheckBox alignDanglingClosingQuotes;
    private String myOldMarginChar;

    @RequiredUIAccess
    protected MultiLineStringCodeStylePanel(CodeStyleSettings settings) {
        super(settings, ScalaBundle.message("multi.line.string.panel.title"));

        openingQuotesOnNewCheckBox = checkBox("multi.line.string.panel.opening.quotes.on.new.line");
        closingQuotesOnNewLine = checkBox("multi.line.string.panel.place.closing.quotes.on.new.line.on.enter.press");
        alignDanglingClosingQuotes = checkBox("multi.line.string.panel.align.dangling.closing.quotes");
        insertMarginChar = checkBox("multi.line.string.panel.insert.margin.char.on.enter");
        processMarginCheckBox = checkBox("multi.line.string.panel.process.margin.on.copy.paste");
        marginCharTextField = TextBox.create("|");
        marginIndentSpinner = IntBox.create(1).withRange(1, Integer.MAX_VALUE);

        panel1 = VerticalLayout.create();
        panel1.add(openingQuotesOnNewCheckBox);
        panel1.add(closingQuotesOnNewLine);
        panel1.add(alignDanglingClosingQuotes);
        panel1.add(insertMarginChar);
        panel1.add(processMarginCheckBox);
        panel1.add(FormBuilder.create()
                .addLabeled(LocalizeValue.of(ScalaBundle.message("multi.line.string.panel.margin.char.value")), marginCharTextField)
                .addLabeled(LocalizeValue.of(ScalaBundle.message("multi.line.string.panel.margin.char.indent")), marginIndentSpinner)
                .build());
        panel1.paddingBuilder().allSet(Space.MEDIUM).apply();

        ScalaCodeStyleSettings scalaSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);
        setSettings(scalaSettings);

        //validation
        // upstream: a key listener rejected typing a second char, here the value is corrected when the field loses focus
        marginCharTextField.addFocusListener(e -> myOldMarginChar = StringUtil.notNullize(marginCharTextField.getValue()));
        marginCharTextField.addBlurListener(e -> {
            String text = StringUtil.notNullize(marginCharTextField.getValue());
            if (text == null || text.length() != 1) {
                marginCharTextField.setValue(myOldMarginChar);
            }
        });
    }

    @RequiredUIAccess
    private static CheckBox checkBox(String key) {
        return CheckBox.create(LocalizeValue.of(ScalaBundle.message(key)));
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        if (!isModified(settings)) return;
        ScalaCodeStyleSettings ss = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        ss.MULTILINE_STRING_OPENING_QUOTES_ON_NEW_LINE = openingQuotesOnNewCheckBox.getValue();
        ss.MULTILINE_STRING_CLOSING_QUOTES_ON_NEW_LINE = closingQuotesOnNewLine.getValue();
        ss.MULTILINE_STRING_ALIGN_DANGLING_CLOSING_QUOTES = alignDanglingClosingQuotes.getValue();
        ss.MULTILINE_STRING_INSERT_MARGIN_ON_ENTER = insertMarginChar.getValue();
        ss.MULTILINE_STRING_PROCESS_MARGIN_ON_COPY_PASTE = processMarginCheckBox.getValue();
        ss.MULTILINE_STRING_MARGIN_CHAR = StringUtil.notNullize(marginCharTextField.getValue());
        ss.MULTILINE_STRING_MARGIN_INDENT = marginIndentSpinner.getValue();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean isModified(CodeStyleSettings settings) {
        ScalaCodeStyleSettings ss = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        if (ss.MULTILINE_STRING_OPENING_QUOTES_ON_NEW_LINE != openingQuotesOnNewCheckBox.getValue()) return true;
        if (ss.MULTILINE_STRING_CLOSING_QUOTES_ON_NEW_LINE != closingQuotesOnNewLine.getValue()) return true;
        if (ss.MULTILINE_STRING_ALIGN_DANGLING_CLOSING_QUOTES != alignDanglingClosingQuotes.getValue()) return true;
        if (ss.MULTILINE_STRING_INSERT_MARGIN_ON_ENTER != insertMarginChar.getValue()) return true;
        if (ss.MULTILINE_STRING_PROCESS_MARGIN_ON_COPY_PASTE != processMarginCheckBox.getValue()) return true;
        if (!ss.MULTILINE_STRING_MARGIN_CHAR.equals(StringUtil.notNullize(marginCharTextField.getValue()))) return true;
        if (ss.MULTILINE_STRING_MARGIN_INDENT != marginIndentSpinner.getValue()) return true;

        return false;
    }

    @Override
    protected Component getPanelInner() {
        return panel1;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        ScalaCodeStyleSettings scalaSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);
        setSettings(scalaSettings);
    }

    private void setSettings(ScalaCodeStyleSettings ss) {
        openingQuotesOnNewCheckBox.setValue(ss.MULTILINE_STRING_OPENING_QUOTES_ON_NEW_LINE);
        closingQuotesOnNewLine.setValue(ss.MULTILINE_STRING_CLOSING_QUOTES_ON_NEW_LINE);
        alignDanglingClosingQuotes.setValue(ss.MULTILINE_STRING_ALIGN_DANGLING_CLOSING_QUOTES);
        insertMarginChar.setValue(ss.MULTILINE_STRING_INSERT_MARGIN_ON_ENTER);
        processMarginCheckBox.setValue(ss.MULTILINE_STRING_PROCESS_MARGIN_ON_COPY_PASTE);
        marginCharTextField.setValue(ss.MULTILINE_STRING_MARGIN_CHAR);
        marginIndentSpinner.setValue(ss.MULTILINE_STRING_MARGIN_INDENT);
    }
}
