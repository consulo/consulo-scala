package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.util.LabeledBuilder;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettingsUtil;

import javax.swing.*;
import java.util.Arrays;

public final class ImportsPanel extends ScalaCodeStylePanelBase {

    private final VerticalLayout contentPanel;
    private final CheckBox addImportStatementInCheckBox;
    private final CheckBox addFullQualifiedImportsCheckBox;
    private final CheckBox sortImportsCheckBox;
    private final RadioGroup<Boolean> sortGroup;
    private final CheckBox importTheShortestPathCheckBox;
    private final CheckBox collectImportsWithTheCheckBox;
    private final IntBox classCountSpinner;
    private final CheckBox doNotChangePathCheckBox;
    private final CheckBox myImportRetativeToBasePackageCheckBox;
    private final ComboBox<Boolean> importSyntaxInSource3;
    private final DefaultListModel<String> myReferencesWithPrefixModel;
    private final DefaultListModel<String> alwaysUsedImportsModel;
    private final DefaultListModel<String> myImportLayoutModel;

    @RequiredUIAccess
    public ImportsPanel(CodeStyleSettings settings) {
        super(settings, ScalaBundle.message("imports.panel.title"));

        classCountSpinner = IntBox.create(1).withRange(1, Integer.MAX_VALUE);
        importSyntaxInSource3 = ComboBox.<Boolean>builder()
                .add(Boolean.FALSE, "Scala 3 (as, *)")
                .add(Boolean.TRUE, "Scala 2 (=>, _)")
                .build();
        addImportStatementInCheckBox = checkBox("imports.panel.add.import.statement.in.closest.block");
        addFullQualifiedImportsCheckBox = checkBox("imports.panel.add.fully.qualified.imports");
        myImportRetativeToBasePackageCheckBox = checkBox("except.for.base.package");
        doNotChangePathCheckBox = checkBox("imports.panel.do.not.change.path.during.optimize.imports.for.local.imports");
        doNotChangePathCheckBox.setValue(true);
        sortImportsCheckBox = checkBox("imports.panel.sort.imports.for.optimize.imports");
        collectImportsWithTheCheckBox = checkBox("imports.panel.merge.imports.with.the.same.prefix.into.one.statement");
        importTheShortestPathCheckBox = checkBox("imports.panel.use.the.shortest.path.when.trying.to.import.reference.with.already.imported.name");
        importTheShortestPathCheckBox.setValue(true);

        sortGroup = RadioGroup.create();
        HorizontalLayout sortButtons = HorizontalLayout.create()
                .add(sortGroup.newButton(LocalizeValue.of(ScalaBundle.message("imports.panel.lexicographically")), Boolean.FALSE))
                .add(sortGroup.newButton(LocalizeValue.of(ScalaBundle.message("imports.panel.scalastyle.consistent")), Boolean.TRUE));
        sortButtons.paddingBuilder().leftSet(Space.XX_LARGE).apply();

        addFullQualifiedImportsCheckBox.addValueListener(event -> myImportRetativeToBasePackageCheckBox.setEnabled(addFullQualifiedImportsCheckBox.getValue()));
        myImportRetativeToBasePackageCheckBox.setEnabled(addFullQualifiedImportsCheckBox.getValue());
        Component basePackage = withContextHelp(myImportRetativeToBasePackageCheckBox, ScalaBundle.message("imports.panel.base.package.help"));
        basePackage.paddingBuilder().leftSet(Space.XX_LARGE).apply();

        // upstream: the pattern lists are Swing lists with a toolbar (no consulo.ui counterpart)
        JBList<String> referencesWithPrefixList = new JBList<>();
        myReferencesWithPrefixModel = new DefaultListModel<>();
        referencesWithPrefixList.setModel(myReferencesWithPrefixModel);
        JPanel importsWithPrefixPanel = ScalaProjectSettingsUtil.getPatternListPanel(referencesWithPrefixList,
                referencesWithPrefixList,
                ScalaBundle.message("imports.panel.add.pattern.to.use.appropriate.classes.only.with.prefix"),
                ScalaBundle.message("imports.panel.use.references.with.prefix"));
        referencesWithPrefixList.getEmptyText().setText(ScalaBundle.message("imports.panel.no.imports.with.prefix"));

        myImportLayoutModel = new DefaultListModel<>();
        JBList<String> importLayoutTable = new JBList<>(myImportLayoutModel);
        JPanel importLayoutPanel = ScalaProjectSettingsUtil.getUnsortedPatternListPanel(importLayoutTable,
                importLayoutTable,
                ScalaBundle.message("imports.panel.add.package.name"),
                ScalaBundle.message("imports.panel.import.layout.manager"));

        JBList<String> alwaysUsedImportsList = new JBList<>();
        alwaysUsedImportsModel = new DefaultListModel<>();
        alwaysUsedImportsList.setModel(alwaysUsedImportsModel);
        JPanel alwaysUsedImportsPanel = ScalaProjectSettingsUtil.getPatternListPanel(alwaysUsedImportsList,
                alwaysUsedImportsList,
                ScalaBundle.message("imports.panel.add.import.to.always.mark.it.as.used"),
                ScalaBundle.message("imports.panel.always.mark.as.used"));
        alwaysUsedImportsList.getEmptyText().setText(ScalaBundle.message("imports.panel.honestly.mark.imports.as.unused"));

        TwoComponentSplitLayout rightLists = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        rightLists.setFirstComponent(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("imports.panel.import.layout")), TargetAWT.wrap(importLayoutPanel)));
        rightLists.setSecondComponent(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("imports.panel.imports.always.marked.as.used")), TargetAWT.wrap(alwaysUsedImportsPanel)));

        TwoComponentSplitLayout lists = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        lists.setFirstComponent(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("imports.panel.classes.to.use.only.with.prefix")), TargetAWT.wrap(importsWithPrefixPanel)));
        lists.setSecondComponent(rightLists);

        contentPanel = VerticalLayout.create();
        contentPanel.add(LabeledBuilder.simple(LocalizeValue.of(ScalaBundle.message("imports.panel.class.count.to.use.import.with")), classCountSpinner));
        contentPanel.add(LabeledBuilder.simple(LocalizeValue.of(ScalaBundle.message("imports.panel.force.scala2.in.source3")), importSyntaxInSource3));
        contentPanel.add(addImportStatementInCheckBox);
        contentPanel.add(addFullQualifiedImportsCheckBox);
        contentPanel.add(basePackage);
        contentPanel.add(doNotChangePathCheckBox);
        contentPanel.add(sortImportsCheckBox);
        contentPanel.add(sortButtons);
        contentPanel.add(collectImportsWithTheCheckBox);
        contentPanel.add(importTheShortestPathCheckBox);
        contentPanel.add(lists);
        contentPanel.paddingBuilder().allSet(Space.MEDIUM).apply();
    }

    @RequiredUIAccess
    private static CheckBox checkBox(String key) {
        return CheckBox.create(LocalizeValue.of(ScalaBundle.message(key)));
    }

    public String[] getPrefixPackages() {
        String[] prefixPackages = new String[myReferencesWithPrefixModel.size()];
        for (int i = 0; i < myReferencesWithPrefixModel.size(); i++) {
            prefixPackages[i] = myReferencesWithPrefixModel.elementAt(i);
        }
        Arrays.sort(prefixPackages);
        return prefixPackages;
    }

    public String[] getAlwaysUsedImports() {
        String[] alwaysUsedImports = new String[alwaysUsedImportsModel.size()];
        for (int i = 0; i < alwaysUsedImportsModel.size(); i++) {
            alwaysUsedImports[i] = alwaysUsedImportsModel.elementAt(i);
        }
        Arrays.sort(alwaysUsedImports);
        return alwaysUsedImports;
    }

    public String[] getImportLayout() {
        String[] importLayout = new String[myImportLayoutModel.size()];
        for (int i = 0; i < myImportLayoutModel.size(); i++) {
            importLayout[i] = myImportLayoutModel.elementAt(i);
        }
        return importLayout;
    }

    private boolean isSortAsScalastyle() {
        return Boolean.TRUE.equals(sortGroup.getValue());
    }

    public boolean isForceScala2ImportSyntaxInSource() {
        return Boolean.TRUE.equals(importSyntaxInSource3.getValue());
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        if (!isModified(settings)) return;

        ScalaCodeStyleSettings scalaCodeStyleSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        scalaCodeStyleSettings.setAddImportMostCloseToReference(addImportStatementInCheckBox.getValue());
        scalaCodeStyleSettings.setAddFullQualifiedImports(addFullQualifiedImportsCheckBox.getValue());
        scalaCodeStyleSettings.setAddImportsRelativeToBasePackage(myImportRetativeToBasePackageCheckBox.getValue());
        scalaCodeStyleSettings.setDoNotChangeLocalImportsOnOptimize(doNotChangePathCheckBox.getValue());
        scalaCodeStyleSettings.setSortImports(sortImportsCheckBox.getValue());
        scalaCodeStyleSettings.setSortAsScalastyle(isSortAsScalastyle());
        scalaCodeStyleSettings.setCollectImports(collectImportsWithTheCheckBox.getValue());
        scalaCodeStyleSettings.setClassCountToUseImportOnDemand(classCountSpinner.getValue());
        scalaCodeStyleSettings.setImportShortestPathForAmbiguousReferences(importTheShortestPathCheckBox.getValue());
        scalaCodeStyleSettings.setImportsWithPrefix(getPrefixPackages());
        scalaCodeStyleSettings.setAlwaysUsedImports(getAlwaysUsedImports());
        scalaCodeStyleSettings.setImportLayout(getImportLayout());
        scalaCodeStyleSettings.setForceScala2ImportSyntaxInSource3(isForceScala2ImportSyntaxInSource());
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        ScalaCodeStyleSettings scalaCodeStyleSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        if (scalaCodeStyleSettings.getClassCountToUseImportOnDemand() !=
                classCountSpinner.getValue()) return true;
        if (scalaCodeStyleSettings.isAddImportMostCloseToReference() !=
                addImportStatementInCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isAddFullQualifiedImports() !=
                addFullQualifiedImportsCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isAddImportsRelativeToBasePackage() !=
                myImportRetativeToBasePackageCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isDoNotChangeLocalImportsOnOptimize() !=
                doNotChangePathCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isSortImports() !=
                sortImportsCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isCollectImports() !=
                collectImportsWithTheCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isImportShortestPathForAmbiguousReferences() !=
                importTheShortestPathCheckBox.getValue()) return true;
        if (scalaCodeStyleSettings.isSortAsScalastyle() !=
                isSortAsScalastyle()) return true;
        if (scalaCodeStyleSettings.isForceScala2ImportSyntaxInSource3() != isForceScala2ImportSyntaxInSource())
            return true;
        if (!Arrays.deepEquals(scalaCodeStyleSettings.getImportsWithPrefix(), getPrefixPackages())) return true;
        if (!Arrays.deepEquals(scalaCodeStyleSettings.getAlwaysUsedImports(), getAlwaysUsedImports())) return true;
        return !Arrays.deepEquals(scalaCodeStyleSettings.getImportLayout(), getImportLayout());
    }

    @Override
    protected Component getPanelInner() {
        return this.contentPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        ScalaCodeStyleSettings scalaCodeStyleSettings = settings.getCustomSettings(ScalaCodeStyleSettings.class);

        addImportStatementInCheckBox.setValue(scalaCodeStyleSettings.isAddImportMostCloseToReference());
        addFullQualifiedImportsCheckBox.setValue(scalaCodeStyleSettings.isAddFullQualifiedImports());
        myImportRetativeToBasePackageCheckBox.setValue(scalaCodeStyleSettings.isAddImportsRelativeToBasePackage());
        doNotChangePathCheckBox.setValue(scalaCodeStyleSettings.isDoNotChangeLocalImportsOnOptimize());
        sortImportsCheckBox.setValue(scalaCodeStyleSettings.isSortImports());
        sortGroup.setValue(scalaCodeStyleSettings.isSortAsScalastyle());
        collectImportsWithTheCheckBox.setValue(scalaCodeStyleSettings.isCollectImports());
        classCountSpinner.setValue(scalaCodeStyleSettings.getClassCountToUseImportOnDemand());
        importTheShortestPathCheckBox.setValue(scalaCodeStyleSettings.isImportShortestPathForAmbiguousReferences());

        myReferencesWithPrefixModel.clear();
        for (String aPackage : scalaCodeStyleSettings.getImportsWithPrefix()) {
            myReferencesWithPrefixModel.add(myReferencesWithPrefixModel.size(), aPackage);
        }

        alwaysUsedImportsModel.clear();
        for (String importPattern : scalaCodeStyleSettings.getAlwaysUsedImports()) {
            alwaysUsedImportsModel.add(alwaysUsedImportsModel.size(), importPattern);
        }

        myImportLayoutModel.clear();
        for (String layoutElement : scalaCodeStyleSettings.getImportLayout()) {
            myImportLayoutModel.add(myImportLayoutModel.size(), layoutElement);
        }

        importSyntaxInSource3.setValue(scalaCodeStyleSettings.isForceScala2ImportSyntaxInSource3());
    }
}
