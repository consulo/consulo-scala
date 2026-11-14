package org.jetbrains.plugins.scala.settings.sections;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.configurable.ConfigurationException;
import consulo.configurable.Settings;
import consulo.dataContext.DataManager;
import consulo.fileEditor.util.FileContentUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.incremental.Highlighting;
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;
import org.jetbrains.plugins.scala.util.ui.ContextHelpLabel;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.DEFAULT_COMPILER_HIGHLIGHTING_DELAY;
import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class EditorSettingsSectionPanel extends SettingsSectionPanel {
    private final CheckBox showImplicitConversionsInCheckBox;
    private final CheckBox showArgumentsToByNameParametersCheckBox;
    private final CheckBox includeBlockExpressionsExpressionsCheckBox;
    private final CheckBox includeLiteralsCheckBox;
    private final CheckBox myAotCompletion;
    private final CheckBox useScalaClassesPriorityCheckBox;
    private final CheckBox enableConversionOnCopyCheckBox;
    private final CheckBox donTShowDialogCheckBox;
    private final CheckBox customScalatestSyntaxHighlightingCheckbox;
    private final CheckBox addOverrideToImplementCheckBox;
    private final CheckBox showNotFoundImplicitArgumentsCheckBox;
    private final ComboBox<ScalaProjectSettings.ScalaCollectionHighlightingLevel> collectionHighlightingChooser;
    private final CheckBox showAmbiguousImplicitArgumentsCheckBox;
    private final ComboBox<ScalaProjectSettings.AliasExportSemantics> aliasSemantics;
    private final CheckBox typeAwareHighlighting;
    private final CheckBox incrementalHighlighting;
    private final CheckBox disableInspections;
    private final HorizontalLayout disableInspectionsHelp;
    private final CheckBox useCompilerTypes;
    private final IntBox compilerHighlightingDelay;
    private final CheckBox showTypeMismatchHintsCheckBox;
    private final ComboBox<ScalaProjectSettings.TypeChecker> typeCheckerScala2;
    private final ComboBox<ScalaProjectSettings.TypeChecker> typeCheckerScala3;
    private final VerticalLayout highlightingOptionsPanel1;
    private final VerticalLayout highlightingOptionsPanel2;
    private final VerticalLayout builtInOptions;
    private final VerticalLayout compilerOptions;
    private final VerticalLayout rootPanel;

    @RequiredUIAccess
    public EditorSettingsSectionPanel(Project project) {
        super(project);

        // upstream: com.intellij.ui.components.ActionLink
        Hyperlink improvesPerformance1 = Hyperlink.create(message("scala.project.settings.form.improves.performance"), e -> selectPerformanceSettings());
        Hyperlink improvesPerformance2 = Hyperlink.create(message("scala.project.settings.form.improves.performance"), e -> selectPerformanceSettings());

        typeCheckerScala2 = createTypeChecker();
        typeCheckerScala3 = createTypeChecker();

        typeAwareHighlighting = CheckBox.create(message("scala.project.settings.form.type.aware.highlighting"));
        incrementalHighlighting = CheckBox.create(message("incremental.highlighting"));
        builtInOptions = VerticalLayout.create();
        builtInOptions.add(withHelp(typeAwareHighlighting, ScalaBundle.message("type.aware.highlighting.help")));
        builtInOptions.add(row(incrementalHighlighting, improvesPerformance1, ContextHelpLabel.createLabel(ScalaBundle.message("incremental.highlighting.help"))));

        disableInspections = CheckBox.create(message("scala.project.settings.form.disable.inspections"));
        disableInspectionsHelp = HorizontalLayout.create();
        useCompilerTypes = CheckBox.create(message("scala.project.settings.form.compiler.types"));
        compilerHighlightingDelay = IntBox.create(DEFAULT_COMPILER_HIGHLIGHTING_DELAY).withRange(100, Integer.MAX_VALUE);
        compilerOptions = VerticalLayout.create();
        compilerOptions.add(row(disableInspections, improvesPerformance2, disableInspectionsHelp));
        compilerOptions.add(withHelp(useCompilerTypes, ScalaBundle.message("use.compiler.types.help")));
        compilerOptions.add(labeledRow(message("scala.project.settings.form.compilation.delay"),
                compilerHighlightingDelay,
                Label.create(message("scala.project.settings.form.milliseconds")),
                ContextHelpLabel.createLabel(ScalaBundle.message("compiler.highlighting.delay.help", DEFAULT_COMPILER_HIGHLIGHTING_DELAY))));

        // Settings for New Projects use the default project, where both modes must be configurable (SCL-26133).
        final var showScala2Setting = project.isDefault() || ScalaProjectUtil.hasScala2(project);
        final var showScala3Setting = project.isDefault() || ScalaProjectUtil.hasScala3(project);
        // The Scala 2: and Scala 3: labels should only be visible if both versions of the language are configured
        // in the project.
        final var showLabels = showScala2Setting && showScala3Setting;

        highlightingOptionsPanel1 = indented(VerticalLayout.create());
        highlightingOptionsPanel2 = indented(VerticalLayout.create());

        VerticalLayout errorHighlightingContent = VerticalLayout.create();
        Component typeCheckerScala2Section = typeCheckerSection("type.checker.label.scala2", typeCheckerScala2, showLabels);
        Component typeCheckerScala3Section = typeCheckerSection("type.checker.label.scala3", typeCheckerScala3, showLabels);
        errorHighlightingContent.add(typeCheckerScala2Section);
        errorHighlightingContent.add(highlightingOptionsPanel1);
        errorHighlightingContent.add(typeCheckerScala3Section);
        errorHighlightingContent.add(highlightingOptionsPanel2);
        // Only the corresponding section should be visible.
        typeCheckerScala2Section.setVisible(showScala2Setting);
        typeCheckerScala3Section.setVisible(showScala3Setting);

        LabeledLayout errorHighlightingSection = LabeledLayout.create(message("scala.project.settings.form.error.highlighting"), errorHighlightingContent);
        // The error highlighting section should not be visible at all in non-Scala projects.
        // upstream: always shown - the type checker choice has no effect while compiler highlighting is not ported
        errorHighlightingSection.setVisible((showScala2Setting || showScala3Setting) && ScalaHighlightingMode.isCompilerHighlightingAvailable());

        showTypeMismatchHintsCheckBox = CheckBox.create(message("scala.project.settings.form.show.type.mismatch.hints"));
        showNotFoundImplicitArgumentsCheckBox = CheckBox.create(message("scala.project.settings.form.show.hints.if.no.implicit.arguments.found"));
        showAmbiguousImplicitArgumentsCheckBox = CheckBox.create(message("scala.project.settings.form.show.hints.if.ambiguous.implicit.arguments.found"));
        aliasSemantics = ComboBox.<ScalaProjectSettings.AliasExportSemantics>builder()
                .add(ScalaProjectSettings.AliasExportSemantics.Definition, message("scala.project.settings.form.alias.definition"))
                .add(ScalaProjectSettings.AliasExportSemantics.Export, message("scala.project.settings.form.alias.export"))
                .build();

        VerticalLayout highlightingContent = VerticalLayout.create();
        highlightingContent.add(showTypeMismatchHintsCheckBox);
        highlightingContent.add(showNotFoundImplicitArgumentsCheckBox);
        highlightingContent.add(showAmbiguousImplicitArgumentsCheckBox);
        highlightingContent.add(labeledRow(message("scala.project.settings.form.alias.export.semantics"),
                aliasSemantics, ContextHelpLabel.createLabel(ScalaBundle.message("scala.project.settings.form.alias.export.semantics.help"))));

        showImplicitConversionsInCheckBox = CheckBox.create(message("scala.project.settings.form.highlight.implicit.conversions"));
        showArgumentsToByNameParametersCheckBox = CheckBox.create(message("scala.project.settings.form.highlight.arguments.to.by.name.parameters"));
        includeBlockExpressionsExpressionsCheckBox = CheckBox.create(message("scala.project.settings.form.include.block.expressions"));
        includeBlockExpressionsExpressionsCheckBox.setToolTipText(LocalizeValue.localizeTODO("Include expressions enclosed in in curly braces"));
        includeLiteralsCheckBox = CheckBox.create(message("scala.project.settings.form.include.literals"));
        includeLiteralsCheckBox.setToolTipText(LocalizeValue.localizeTODO("Include string, number, etc"));
        customScalatestSyntaxHighlightingCheckbox = CheckBox.create(message("scala.project.settings.form.custom.scalatest.keywords.highlighting"));
        collectionHighlightingChooser = ComboBox.<ScalaProjectSettings.ScalaCollectionHighlightingLevel>builder()
                .add(ScalaProjectSettings.ScalaCollectionHighlightingLevel.None, message("scala.collection.highlighting.type.none"))
                .add(ScalaProjectSettings.ScalaCollectionHighlightingLevel.OnlyNonQualified, message("scala.collection.highlighting.type.only.non.qualified"))
                .add(ScalaProjectSettings.ScalaCollectionHighlightingLevel.All, message("scala.collection.highlighting.type.all"))
                .build();
        myAotCompletion = CheckBox.create(message("scala.project.settings.form.ahead.of.time.completion"));
        useScalaClassesPriorityCheckBox = CheckBox.create(message("scala.project.settings.form.use.scala.classes.priority.over.java"));
        enableConversionOnCopyCheckBox = CheckBox.create(message("scala.project.settings.form.convert.java.code.to.scala.on.copy.paste"));
        donTShowDialogCheckBox = CheckBox.create(message("scala.project.settings.form.automatically.convert.to.scala.code.without.dialog"));
        addOverrideToImplementCheckBox = CheckBox.create(message("scala.project.settings.form.add.override.keyword.to.method.implementation"));

        VerticalLayout advancedPanel = VerticalLayout.create();
        advancedPanel.add(showImplicitConversionsInCheckBox);
        advancedPanel.add(showArgumentsToByNameParametersCheckBox);
        advancedPanel.add(indented(includeBlockExpressionsExpressionsCheckBox));
        advancedPanel.add(indented(includeLiteralsCheckBox));
        advancedPanel.add(customScalatestSyntaxHighlightingCheckbox);
        advancedPanel.add(labeledRow(message("scala.project.settings.form.collection.type.highlighting.option"), collectionHighlightingChooser));
        advancedPanel.add(myAotCompletion);
        advancedPanel.add(useScalaClassesPriorityCheckBox);
        advancedPanel.add(enableConversionOnCopyCheckBox);
        advancedPanel.add(indented(donTShowDialogCheckBox));
        advancedPanel.add(addOverrideToImplementCheckBox);

        rootPanel = VerticalLayout.create();
        rootPanel.add(errorHighlightingSection);
        rootPanel.add(LabeledLayout.create(message("scala.project.settings.form.highlighting"), highlightingContent));
        // upstream: Kotlin UI DSL collapsible group
        rootPanel.add(FoldoutLayout.create(message("scala.project.settings.form.advanced"), advancedPanel, false));

        updateCompilerSettings();
    }

    @RequiredUIAccess
    private ComboBox<ScalaProjectSettings.TypeChecker> createTypeChecker() {
        ComboBox<ScalaProjectSettings.TypeChecker> typeChecker = ComboBox.<ScalaProjectSettings.TypeChecker>builder()
                .add(ScalaProjectSettings.TypeChecker.BuiltIn, message("type.checker.built.in"))
                .add(ScalaProjectSettings.TypeChecker.Compiler, message("type.checker.compiler"))
                .build();
        typeChecker.addValueListener(e -> updateCompilerSettings());
        return typeChecker;
    }

    @RequiredUIAccess
    private static Component typeCheckerSection(String labelKey, ComboBox<ScalaProjectSettings.TypeChecker> typeChecker, boolean showLabel) {
        Label label = Label.create(message(labelKey));
        label.setTarget(typeChecker);
        label.setVisible(showLabel);
        return row(label, typeChecker, ContextHelpLabel.createLabel(ScalaBundle.message("type.checker.help")));
    }

    private static void selectPerformanceSettings() {
        DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(context -> {
            Settings settings = context.getData(Settings.KEY);
            if (settings != null) {
                settings.select(PerformanceSettingsSectionConfigurable.class);
            }
        });
    }

    @RequiredUIAccess
    private void updateCompilerSettings() {
        // called from the type checker listeners while the panel is being built
        if (highlightingOptionsPanel2 == null) return;

        boolean modeScala2 = typeCheckerScala2.getValue() == ScalaProjectSettings.TypeChecker.Compiler;
        boolean modeScala3 = typeCheckerScala3.getValue() == ScalaProjectSettings.TypeChecker.Compiler;

        // Keep the options layout consistent with the default-project visibility above (SCL-26133).
        boolean showScala2Setting = myProject.isDefault() || ScalaProjectUtil.hasScala2(myProject);
        boolean showScala3Setting = myProject.isDefault() || ScalaProjectUtil.hasScala3(myProject);
        boolean modesDiffer = showScala2Setting && showScala3Setting && modeScala2 != modeScala3;
        boolean mode = showScala2Setting ? modeScala2 : modeScala3;

        boolean hasBultInHighlighting = modesDiffer || !mode;
        boolean isCompilerHighlightingOnly = !hasBultInHighlighting;

        if (!hasBultInHighlighting) {
            incrementalHighlighting.setValue(false);
        }

        disableInspections.setEnabled(isCompilerHighlightingOnly);
        disableInspections.setToolTipText(isCompilerHighlightingOnly ? LocalizeValue.empty() : message("disable.inspections.tooltip"));
        disableInspectionsHelp.removeAll();
        disableInspectionsHelp.add(ContextHelpLabel.createLabel(ScalaBundle.message("disable.inspections.help", isCompilerHighlightingOnly ? "" : ScalaBundle.message("disable.inspections.tooltip"))));
        if (!isCompilerHighlightingOnly) {
            disableInspections.setValue(false);
        }

        highlightingOptionsPanel1.removeAll();
        highlightingOptionsPanel2.removeAll();

        if (modesDiffer) {
            highlightingOptionsPanel1.add(modeScala2 ? compilerOptions : builtInOptions);
            highlightingOptionsPanel2.add(modeScala3 ? compilerOptions : builtInOptions);
        } else {
            highlightingOptionsPanel2.add(mode ? compilerOptions : builtInOptions);
        }

        highlightingOptionsPanel1.setVisible(modesDiffer);
    }

    @Override
    Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.isEnableJavaToScalaConversion() != enableConversionOnCopyCheckBox.getValue() ||
                scalaProjectSettings.isAddOverrideToImplementInConverter() != addOverrideToImplementCheckBox.getValue() ||
                scalaProjectSettings.isDontShowConversionDialog() != donTShowDialogCheckBox.getValue() ||

                scalaProjectSettings.isShowImplicitConversions() != showImplicitConversionsInCheckBox.getValue() ||
                scalaProjectSettings.isTypeMismatchHints() != showTypeMismatchHintsCheckBox.getValue() ||
                scalaProjectSettings.isShowNotFoundImplicitArguments() != showNotFoundImplicitArgumentsCheckBox.getValue() ||
                scalaProjectSettings.isShowAmbiguousImplicitArguments() != showAmbiguousImplicitArgumentsCheckBox.getValue() ||
                scalaProjectSettings.isShowArgumentsToByNameParams() != showArgumentsToByNameParametersCheckBox.getValue() ||
                scalaProjectSettings.isCustomScalatestSyntaxHighlighting() != customScalatestSyntaxHighlightingCheckbox.getValue() ||
                scalaProjectSettings.isIncludeBlockExpressions() != includeBlockExpressionsExpressionsCheckBox.getValue() ||
                scalaProjectSettings.isIncludeLiterals() != includeLiteralsCheckBox.getValue() ||

                scalaProjectSettings.isAotCompletion() != myAotCompletion.getValue() ||
                scalaProjectSettings.isScalaPriority() != useScalaClassesPriorityCheckBox.getValue() ||
                scalaProjectSettings.getAliasSemantics() != aliasSemantics.getValue() ||
                scalaProjectSettings.getCollectionTypeHighlightingLevel() != collectionHighlightingChooser.getValue() ||

                scalaProjectSettings.isCompilerHighlightingScala2() != (typeCheckerScala2.getValue() == ScalaProjectSettings.TypeChecker.Compiler) ||
                scalaProjectSettings.isCompilerHighlightingScala3() != (typeCheckerScala3.getValue() == ScalaProjectSettings.TypeChecker.Compiler) ||
                scalaProjectSettings.isTypeAwareHighlightingEnabled() != typeAwareHighlighting.getValue() ||
                scalaProjectSettings.isIncrementalHighlighting() != incrementalHighlighting.getValue() ||
                scalaProjectSettings.isDisableInspections() != disableInspections.getValue() ||
                scalaProjectSettings.isUseCompilerTypes() != useCompilerTypes.getValue() ||
                scalaProjectSettings.getCompilerHighlightingDelay() != getValue(compilerHighlightingDelay, DEFAULT_COMPILER_HIGHLIGHTING_DELAY)
                ;
    }

    @Override
    void apply() throws ConfigurationException {
        if (!isModified()) return;

        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setEnableJavaToScalaConversion(enableConversionOnCopyCheckBox.getValue());
        scalaProjectSettings.setAddOverrideToImplementInConverter(addOverrideToImplementCheckBox.getValue());
        scalaProjectSettings.setDontShowConversionDialog(donTShowDialogCheckBox.getValue());

        scalaProjectSettings.setShowImplicitConversions(showImplicitConversionsInCheckBox.getValue());
        scalaProjectSettings.setTypeMismatchHints(showTypeMismatchHintsCheckBox.getValue());
        scalaProjectSettings.setShowNotFoundImplicitArguments(showNotFoundImplicitArgumentsCheckBox.getValue());
        scalaProjectSettings.setShowAmbiguousImplicitArguments(showAmbiguousImplicitArgumentsCheckBox.getValue());
        scalaProjectSettings.setShowArgumentsToByNameParams(showArgumentsToByNameParametersCheckBox.getValue());
        if (scalaProjectSettings.isCustomScalatestSyntaxHighlighting() != customScalatestSyntaxHighlightingCheckbox.getValue()) {
            //settings have changed. but actual highlighting will only change on next pass of highlighting visitors
            Alerts.okInfo(message("changes.in.scalatest.highlighting.will.be.processed...")).showAsync(rootPanel);
        }
        scalaProjectSettings.setCustomScalatestSyntaxHighlighting(customScalatestSyntaxHighlightingCheckbox.getValue());
        scalaProjectSettings.setIncludeBlockExpressions(includeBlockExpressionsExpressionsCheckBox.getValue());
        scalaProjectSettings.setIncludeLiterals(includeLiteralsCheckBox.getValue());

        scalaProjectSettings.setAotCOmpletion(myAotCompletion.getValue());
        scalaProjectSettings.setScalaPriority(useScalaClassesPriorityCheckBox.getValue());
        scalaProjectSettings.setAliasSemantics(aliasSemantics.getValue());
        scalaProjectSettings.setCollectionTypeHighlightingLevel(collectionHighlightingChooser.getValue());

        scalaProjectSettings.setCompilerHighlightingScala2(typeCheckerScala2.getValue() == ScalaProjectSettings.TypeChecker.Compiler);
        scalaProjectSettings.setCompilerHighlightingScala3(typeCheckerScala3.getValue() == ScalaProjectSettings.TypeChecker.Compiler);
        if (scalaProjectSettings.isTypeAwareHighlightingEnabled() != typeAwareHighlighting.getValue()) {
            scalaProjectSettings.setTypeAwareHighlightingEnabled(typeAwareHighlighting.getValue());
            ApplicationManager.getApplication().invokeLater(() -> {
                Editor[] openEditors = EditorFactory.getInstance().getAllEditors();
                List<VirtualFile> vFiles = Arrays.stream(openEditors).map(Editor::getVirtualFile).filter(Objects::nonNull).toList();
                FileContentUtil.reparseFiles(myProject, vFiles, true);
            }, ModalityState.nonModal());
        }
        boolean wasEnabled = scalaProjectSettings.isIncrementalHighlighting();
        boolean enabled = incrementalHighlighting.getValue();
        scalaProjectSettings.setIncrementalHighlighting(enabled);
        if (enabled != wasEnabled) {
            Highlighting.update(enabled, myProject);
        }
        scalaProjectSettings.setDisableInspections(disableInspections.getValue());
        scalaProjectSettings.setUseCompilerTypes(useCompilerTypes.getValue());
        scalaProjectSettings.setCompilerHighlightingDelay(getValue(compilerHighlightingDelay, DEFAULT_COMPILER_HIGHLIGHTING_DELAY));
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        enableConversionOnCopyCheckBox.setValue(scalaProjectSettings.isEnableJavaToScalaConversion());
        addOverrideToImplementCheckBox.setValue(scalaProjectSettings.isAddOverrideToImplementInConverter());
        donTShowDialogCheckBox.setValue(scalaProjectSettings.isDontShowConversionDialog());

        showImplicitConversionsInCheckBox.setValue(scalaProjectSettings.isShowImplicitConversions());
        showTypeMismatchHintsCheckBox.setValue(scalaProjectSettings.isTypeMismatchHints());
        showNotFoundImplicitArgumentsCheckBox.setValue(scalaProjectSettings.isShowNotFoundImplicitArguments());
        showAmbiguousImplicitArgumentsCheckBox.setValue(scalaProjectSettings.isShowAmbiguousImplicitArguments());
        showArgumentsToByNameParametersCheckBox.setValue(scalaProjectSettings.isShowArgumentsToByNameParams());
        customScalatestSyntaxHighlightingCheckbox.setValue(scalaProjectSettings.isCustomScalatestSyntaxHighlighting());
        includeBlockExpressionsExpressionsCheckBox.setValue(scalaProjectSettings.isIncludeBlockExpressions());
        includeLiteralsCheckBox.setValue(scalaProjectSettings.isIncludeLiterals());

        myAotCompletion.setValue(scalaProjectSettings.isAotCompletion());
        useScalaClassesPriorityCheckBox.setValue(scalaProjectSettings.isScalaPriority());
        aliasSemantics.setValue(scalaProjectSettings.getAliasSemantics());
        collectionHighlightingChooser.setValue(scalaProjectSettings.getCollectionTypeHighlightingLevel());

        typeCheckerScala2.setValue(scalaProjectSettings.isCompilerHighlightingScala2() ? ScalaProjectSettings.TypeChecker.Compiler : ScalaProjectSettings.TypeChecker.BuiltIn);
        typeCheckerScala3.setValue(scalaProjectSettings.isCompilerHighlightingScala3() ? ScalaProjectSettings.TypeChecker.Compiler : ScalaProjectSettings.TypeChecker.BuiltIn);
        typeAwareHighlighting.setValue(scalaProjectSettings.isTypeAwareHighlightingEnabled());
        incrementalHighlighting.setValue(scalaProjectSettings.isIncrementalHighlighting());
        disableInspections.setValue(scalaProjectSettings.isDisableInspections());
        useCompilerTypes.setValue(scalaProjectSettings.isUseCompilerTypes());
        compilerHighlightingDelay.setValue(scalaProjectSettings.getCompilerHighlightingDelay());
        updateCompilerSettings();
    }
}
