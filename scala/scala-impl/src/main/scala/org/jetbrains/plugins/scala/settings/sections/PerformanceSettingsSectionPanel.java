package org.jetbrains.plugins.scala.settings.sections;

import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class PerformanceSettingsSectionPanel extends SettingsSectionPanel {
    private final IntBox implicitParametersSearchDepthSpinner;
    private final CheckBox treatDocCommentAsBlockComment;
    private final CheckBox myDisableLanguageInjection;
    private final CheckBox myDontCacheCompound;
    private final CheckBox searchAllSymbolsIncludeCheckBox;
    private final ComboBox<ScalaProjectSettings.ScalaMetaMode> scalaMetaMode;
    private final CheckBox metaTrimBodies;
    private final ComboBox<ScalaProjectSettings.Ivy2IndexingMode> ivy2IndexingModeCBB;
    private final VerticalLayout rootPanel;

    @RequiredUIAccess
    protected PerformanceSettingsSectionPanel(Project project) {
        super(project);

        // upstream: JEditorPane wrapping at the panel width, HtmlLabel needs an explicit width to wrap the lines
        HtmlLabel performanceGuidelines = HtmlLabel.create(message("scala.project.settings.form.performance.guidelines")
                .map(html -> "<div style=\"width: 600px\">" + html.replace("<html><body>", "").replace("</body></html>", "") + "</div>"));
        performanceGuidelines.addHyperlinkListener(event -> {
            String url = event.getDescription();
            if (!url.isEmpty()) {
                Platform.current().openInBrowser(url);
            }
        });

        implicitParametersSearchDepthSpinner = IntBox.create();

        scalaMetaMode = ComboBox.<ScalaProjectSettings.ScalaMetaMode>builder()
                .add(ScalaProjectSettings.ScalaMetaMode.Enabled, message("scala.meta.mode.enabled"))
                .add(ScalaProjectSettings.ScalaMetaMode.Disabled, message("scala.meta.mode.disabled"))
                .add(ScalaProjectSettings.ScalaMetaMode.Manual, message("scala.meta.mode.manual"))
                .build();
        scalaMetaMode.setToolTipText(message("scala.project.settings.form.scala.meta.settings.modeOptions.tooltip"));

        ivy2IndexingModeCBB = ComboBox.<ScalaProjectSettings.Ivy2IndexingMode>builder()
                .add(ScalaProjectSettings.Ivy2IndexingMode.Disabled, message("ivy2.indexing.mode.disabled"))
                .add(ScalaProjectSettings.Ivy2IndexingMode.Metadata, message("ivy2.indexing.mode.metadata"))
                .add(ScalaProjectSettings.Ivy2IndexingMode.Classes, message("ivy2.indexing.mode.classes"))
                .build();
        ivy2IndexingModeCBB.setToolTipText(message("scala.project.settings.form.sbt.index.ivy2.mode.hint"));

        metaTrimBodies = CheckBox.create(message("scala.project.settings.form.scala.meta.settings.trimBodies.caption"));
        metaTrimBodies.setToolTipText(message("scala.project.settings.form.scala.meta.settings.trimBodies.tooltip"));
        searchAllSymbolsIncludeCheckBox = CheckBox.create(message("scala.project.settings.form.search.all.symbols"));
        treatDocCommentAsBlockComment = CheckBox.create(message("scala.project.settings.form.disable.parsing.of.documentation.comments"));
        myDisableLanguageInjection = CheckBox.create(message("scala.project.settings.form.disable.language.injection.in.scala.files"));
        myDontCacheCompound = CheckBox.create(message("scala.project.settings.form.dont.cache.compound.types"));

        Label scalaMetaLabel = Label.create(message("scala.project.settings.form.scala.meta.settings.annot212"));
        scalaMetaLabel.setToolTipText(message("scala.project.settings.form.scala.meta.settings.annot212.tooltip"));
        scalaMetaLabel.setTarget(scalaMetaMode);
        Label ivy2Label = Label.create(message("scala.project.settings.form.sbt.index.ivy2.mode"));
        ivy2Label.setToolTipText(message("scala.project.settings.form.sbt.index.ivy2.mode.hint"));
        ivy2Label.setTarget(ivy2IndexingModeCBB);

        VerticalLayout advancedPanel = VerticalLayout.create();
        advancedPanel.add(labeledRow(message("scala.project.settings.form.implicit.parameters.search.depth"), implicitParametersSearchDepthSpinner));
        advancedPanel.add(row(scalaMetaLabel, scalaMetaMode));
        advancedPanel.add(row(ivy2Label, ivy2IndexingModeCBB));
        advancedPanel.add(metaTrimBodies);
        advancedPanel.add(searchAllSymbolsIncludeCheckBox);
        advancedPanel.add(withHelp(treatDocCommentAsBlockComment, ScalaBundle.message("scala.project.settings.form.disable.parsing.of.documentation.comments.help")));
        advancedPanel.add(withHelp(myDisableLanguageInjection, ScalaBundle.message("scala.project.settings.form.disable.language.injection.in.scala.files.help")));
        advancedPanel.add(withHelp(myDontCacheCompound, ScalaBundle.message("scala.project.settings.form.dont.cache.compound.types.help")));

        rootPanel = VerticalLayout.create();
        rootPanel.add(performanceGuidelines);
        // upstream: Kotlin UI DSL collapsible group
        rootPanel.add(FoldoutLayout.create(message("scala.project.settings.form.performance.advanced"), advancedPanel, false));

        reset();
    }

    @Override
    Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.getImplicitParametersSearchDepth() != getValue(implicitParametersSearchDepthSpinner, 0) ||
                scalaProjectSettings.isSearchAllSymbols() != searchAllSymbolsIncludeCheckBox.getValue() ||
                scalaProjectSettings.isTreatDocCommentAsBlockComment() != treatDocCommentAsBlockComment.getValue() ||
                scalaProjectSettings.isDisableLangInjection() != myDisableLanguageInjection.getValue() ||
                scalaProjectSettings.isDontCacheCompoundTypes() != myDontCacheCompound.getValue() ||
                !scalaProjectSettings.getScalaMetaMode().equals(scalaMetaMode.getValue()) ||
                scalaProjectSettings.isMetaTrimMethodBodies() != metaTrimBodies.getValue() ||
                !scalaProjectSettings.getIvy2IndexingMode().equals(ivy2IndexingModeCBB.getValue())
                ;
    }

    @Override
    void apply() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setImplicitParametersSearchDepth(getValue(implicitParametersSearchDepthSpinner, 0));
        scalaProjectSettings.setSearchAllSymbols(searchAllSymbolsIncludeCheckBox.getValue());
        scalaProjectSettings.setTreatDocCommentAsBlockComment(treatDocCommentAsBlockComment.getValue());
        scalaProjectSettings.setDisableLangInjection(myDisableLanguageInjection.getValue());
        scalaProjectSettings.setDontCacheCompoundTypes(myDontCacheCompound.getValue());
        scalaProjectSettings.setScalaMetaMode(scalaMetaMode.getValue());
        scalaProjectSettings.setMetaTrimMethodBodies(metaTrimBodies.getValue());
        scalaProjectSettings.setIvy2IndexingMode(ivy2IndexingModeCBB.getValue());
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        implicitParametersSearchDepthSpinner.setValue(scalaProjectSettings.getImplicitParametersSearchDepth());
        searchAllSymbolsIncludeCheckBox.setValue(scalaProjectSettings.isSearchAllSymbols());
        treatDocCommentAsBlockComment.setValue(scalaProjectSettings.isTreatDocCommentAsBlockComment());
        myDisableLanguageInjection.setValue(scalaProjectSettings.isDisableLangInjection());
        myDontCacheCompound.setValue(scalaProjectSettings.isDontCacheCompoundTypes());
        scalaMetaMode.setValue(scalaProjectSettings.getScalaMetaMode());
        metaTrimBodies.setValue(scalaProjectSettings.isMetaTrimMethodBodies());
        ivy2IndexingModeCBB.setValue(scalaProjectSettings.getIvy2IndexingMode());
    }
}
