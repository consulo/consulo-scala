package org.jetbrains.plugins.scala.settings.sections;

import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings;
import org.jetbrains.plugins.scala.settings.XRayUtils;
import org.jetbrains.plugins.scala.settings.XRayWidgetMode;

public class XRayModeSettingsSectionPanel extends SettingsSectionPanel {
    private final CheckBox myShowMethodChainHintsCheckbox;
    private final CheckBox myShowIndentGuidesCheckbox;
    private final CheckBox myShowMethodSeparatorsCheckbox;
    private final CheckBox myShowImplicitHintsCheckbox;
    private final CheckBox myShowTypeHintsCheckbox;
    private final CheckBox myShowMethodResultsCheckbox;
    private final CheckBox myShowMemberVariablesCheckbox;
    private final CheckBox myShowLocalVariablesCheckbox;
    private final CheckBox myShowLambdaParametersCheckbox;
    private final CheckBox myShowLambdaPlaceholdersCheckbox;
    private final CheckBox myShowVariablePatternsCheckbox;
    private final CheckBox myShowParameterHintsCheckbox;
    private final CheckBox myShowArgumentHintsCheckbox;
    private final CheckBox myShowApplyMethodHintsCheckbox;
    private final CheckBox myForAllParametersCheckbox;
    private final CheckBox myDoublePressAndHoldCheckbox;
    private final CheckBox myPressAndHoldCheckbox;
    private final CheckBox myShowTypeArgumentsCheckbox;
    private final ComboBox<XRayWidgetMode> myWidgetModeCombobox;
    private final VerticalLayout rootPanel;

    @RequiredUIAccess
    protected XRayModeSettingsSectionPanel(Project project) {
        super(project);

        String key = XRayUtils.xRayActionKeyText();
        myDoublePressAndHoldCheckbox = CheckBox.create(message("scala.project.settings.form.xray.double.press.and.hold", key));
        myPressAndHoldCheckbox = CheckBox.create(message("scala.project.settings.form.xray.press.and.hold", key));

        myShowParameterHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.parameter.name.hints"));
        myForAllParametersCheckbox = CheckBox.create(message("scala.project.settings.form.xray.for.all.parameters"));
        myForAllParametersCheckbox.setToolTipText(message("scala.project.settings.form.xray.for.all.parameters.tooltip"));
        myShowArgumentHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.by-name.argument.hints"));
        myShowApplyMethodHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.apply.method.hints"));
        myShowTypeHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.type.hints"));
        myShowMemberVariablesCheckbox = CheckBox.create(message("scala.project.settings.form.xray.member.variables"));
        myShowLocalVariablesCheckbox = CheckBox.create(message("scala.project.settings.form.xray.local.variables"));
        myShowMethodResultsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.method.results"));
        myShowLambdaParametersCheckbox = CheckBox.create(message("scala.project.settings.form.xray.lambda.parameters"));
        myShowLambdaPlaceholdersCheckbox = CheckBox.create(message("scala.project.settings.form.xray.lambda.placeholders"));
        myShowVariablePatternsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.variable.patterns"));
        myShowMethodChainHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.method.chain.hints"));
        myShowTypeArgumentsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.type.arguments.hints"));
        myShowImplicitHintsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.implicit.hints"));
        myShowIndentGuidesCheckbox = CheckBox.create(message("scala.project.settings.form.xray.indent.guides"));
        myShowMethodSeparatorsCheckbox = CheckBox.create(message("scala.project.settings.form.xray.method.separators"));

        myWidgetModeCombobox = ComboBox.<XRayWidgetMode>builder()
                .add(XRayWidgetMode.ALWAYS, message("scala.project.settings.form.xray.widget.display.always"))
                .add(XRayWidgetMode.WHEN_ACTIVE, message("scala.project.settings.form.xray.widget.display.when.active"))
                .add(XRayWidgetMode.NEVER, message("scala.project.settings.form.xray.widget.display.never"))
                .build();

        myShowParameterHintsCheckbox.addValueListener(e -> updateParameterHintCheckboxes());
        myShowTypeHintsCheckbox.addValueListener(e -> updateTypeHintCheckboxes());

        VerticalLayout activate = indented(VerticalLayout.create());
        activate.add(myDoublePressAndHoldCheckbox);
        activate.add(myPressAndHoldCheckbox);

        VerticalLayout parameterHints = indented(VerticalLayout.create());
        parameterHints.add(myForAllParametersCheckbox);

        VerticalLayout typeHints = indented(VerticalLayout.create());
        typeHints.add(myShowMemberVariablesCheckbox);
        typeHints.add(myShowLocalVariablesCheckbox);
        typeHints.add(myShowMethodResultsCheckbox);
        typeHints.add(myShowLambdaParametersCheckbox);
        typeHints.add(myShowLambdaPlaceholdersCheckbox);
        typeHints.add(myShowVariablePatternsCheckbox);

        VerticalLayout show = indented(VerticalLayout.create());
        show.add(myShowParameterHintsCheckbox);
        show.add(parameterHints);
        show.add(myShowArgumentHintsCheckbox);
        show.add(myShowApplyMethodHintsCheckbox);
        show.add(myShowTypeHintsCheckbox);
        show.add(typeHints);
        show.add(myShowMethodChainHintsCheckbox);
        show.add(myShowTypeArgumentsCheckbox);
        show.add(myShowImplicitHintsCheckbox);
        show.add(myShowIndentGuidesCheckbox);
        show.add(myShowMethodSeparatorsCheckbox);

        rootPanel = VerticalLayout.create();
        rootPanel.add(Label.create(message("scala.project.settings.form.xray.activate")));
        rootPanel.add(activate);
        rootPanel.add(Label.create(message("scala.project.settings.form.xray.show")));
        rootPanel.add(show);
        rootPanel.add(Label.create(message("scala.project.settings.form.xray.widget")));
        rootPanel.add(indented(labeledRow(message("scala.project.settings.form.xray.widget.display"), myWidgetModeCombobox)));

        reset();
        updateParameterHintCheckboxes();
        updateTypeHintCheckboxes();
    }

    @Override
    Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        ScalaApplicationSettings scalaApplicationSettings = ScalaApplicationSettings.getInstance();
        return scalaApplicationSettings.XRAY_DOUBLE_PRESS_AND_HOLD != myDoublePressAndHoldCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_PRESS_AND_HOLD != myPressAndHoldCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_PARAMETER_HINTS != myShowParameterHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_ARGUMENT_HINTS != myShowArgumentHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_APPLY_METHOD_HINTS != myShowApplyMethodHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_TYPE_HINTS != myShowTypeHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_MEMBER_VARIABLE_HINTS != myShowMemberVariablesCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_LOCAL_VARIABLE_HINTS != myShowLocalVariablesCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_METHOD_RESULT_HINTS != myShowMethodResultsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_LAMBDA_PARAMETER_HINTS != myShowLambdaParametersCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_LAMBDA_PLACEHOLDER_HINTS != myShowLambdaPlaceholdersCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_VARIABLE_PATTERN_HINTS != myShowVariablePatternsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_TYPE_ARGUMENT_HINTS != myShowTypeArgumentsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_METHOD_CHAIN_HINTS != myShowMethodChainHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_IMPLICIT_HINTS != myShowImplicitHintsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS != myForAllParametersCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_INDENT_GUIDES != myShowIndentGuidesCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_SHOW_METHOD_SEPARATORS != myShowMethodSeparatorsCheckbox.getValue() ||
                scalaApplicationSettings.XRAY_WIDGET_MODE != myWidgetModeCombobox.getValue();

    }

    @Override
    void apply() {
        ScalaApplicationSettings scalaApplicationSettings = ScalaApplicationSettings.getInstance();
        scalaApplicationSettings.XRAY_DOUBLE_PRESS_AND_HOLD = myDoublePressAndHoldCheckbox.getValue();
        scalaApplicationSettings.XRAY_PRESS_AND_HOLD = myPressAndHoldCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_PARAMETER_HINTS = myShowParameterHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_ARGUMENT_HINTS = myShowArgumentHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_APPLY_METHOD_HINTS = myShowApplyMethodHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_TYPE_HINTS = myShowTypeHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_MEMBER_VARIABLE_HINTS = myShowMemberVariablesCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_LOCAL_VARIABLE_HINTS = myShowLocalVariablesCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_METHOD_RESULT_HINTS = myShowMethodResultsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_LAMBDA_PARAMETER_HINTS = myShowLambdaParametersCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_LAMBDA_PLACEHOLDER_HINTS = myShowLambdaPlaceholdersCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_VARIABLE_PATTERN_HINTS = myShowVariablePatternsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_TYPE_ARGUMENT_HINTS = myShowTypeArgumentsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_METHOD_CHAIN_HINTS = myShowMethodChainHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_IMPLICIT_HINTS = myShowImplicitHintsCheckbox.getValue();
        scalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS = myForAllParametersCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_INDENT_GUIDES = myShowIndentGuidesCheckbox.getValue();
        scalaApplicationSettings.XRAY_SHOW_METHOD_SEPARATORS = myShowMethodSeparatorsCheckbox.getValue();
        scalaApplicationSettings.XRAY_WIDGET_MODE = myWidgetModeCombobox.getValue();
    }

    @Override
    void reset() {
        ScalaApplicationSettings scalaApplicationSettings = ScalaApplicationSettings.getInstance();
        myDoublePressAndHoldCheckbox.setValue(scalaApplicationSettings.XRAY_DOUBLE_PRESS_AND_HOLD);
        myPressAndHoldCheckbox.setValue(scalaApplicationSettings.XRAY_PRESS_AND_HOLD);
        myShowParameterHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_PARAMETER_HINTS);
        myShowArgumentHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_ARGUMENT_HINTS);
        myShowApplyMethodHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_APPLY_METHOD_HINTS);
        myShowTypeHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_TYPE_HINTS);
        myShowMemberVariablesCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_MEMBER_VARIABLE_HINTS);
        myShowLocalVariablesCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_LOCAL_VARIABLE_HINTS);
        myShowMethodResultsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_METHOD_RESULT_HINTS);
        myShowLambdaParametersCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_LAMBDA_PARAMETER_HINTS);
        myShowLambdaPlaceholdersCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_LAMBDA_PLACEHOLDER_HINTS);
        myShowVariablePatternsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_VARIABLE_PATTERN_HINTS);
        myShowTypeArgumentsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_TYPE_ARGUMENT_HINTS);
        myShowMethodChainHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_METHOD_CHAIN_HINTS);
        myShowImplicitHintsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_IMPLICIT_HINTS);
        myForAllParametersCheckbox.setValue(scalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS);
        myShowIndentGuidesCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_INDENT_GUIDES);
        myShowMethodSeparatorsCheckbox.setValue(scalaApplicationSettings.XRAY_SHOW_METHOD_SEPARATORS);
        myWidgetModeCombobox.setValue(scalaApplicationSettings.XRAY_WIDGET_MODE);
    }

    @RequiredUIAccess
    private void updateParameterHintCheckboxes() {
        boolean b = myShowParameterHintsCheckbox.getValue();
        myForAllParametersCheckbox.setEnabled(b);
    }

    @RequiredUIAccess
    private void updateTypeHintCheckboxes() {
        boolean b = myShowTypeHintsCheckbox.getValue();
        myShowMemberVariablesCheckbox.setEnabled(b);
        myShowLocalVariablesCheckbox.setEnabled(b);
        myShowMethodResultsCheckbox.setEnabled(b);
        myShowMethodResultsCheckbox.setEnabled(b);
        myShowLambdaParametersCheckbox.setEnabled(b);
        myShowLambdaPlaceholdersCheckbox.setEnabled(b);
        myShowVariablePatternsCheckbox.setEnabled(b);
    }
}
