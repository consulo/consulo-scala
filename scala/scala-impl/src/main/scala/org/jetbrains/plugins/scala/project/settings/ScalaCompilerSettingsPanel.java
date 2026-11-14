package org.jetbrains.plugins.scala.project.settings;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.compiler.data.CompileOrder;
import org.jetbrains.plugins.scala.compiler.data.DebuggingInfoLevel;
import org.jetbrains.plugins.scala.compiler.data.ScalaCompilerSettingsState;
import org.jetbrains.plugins.scala.project.MyPathEditor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ScalaCompilerSettingsPanel {
    private final VerticalLayout myContentPanel;
    private final TextBoxWithExpandAction myAdditionalCompilerOptions;
    private final ComboBox<DebuggingInfoLevel> myDebuggingInfoLevel;
    private final CheckBox myWarnings;
    private final CheckBox myDeprecationWarnings;
    private final CheckBox myUncheckedWarnings;
    private final CheckBox myOptimiseBytecode;
    private final CheckBox myExplainTypeErrors;
    private final CheckBox myContinuations;
    private final ComboBox<CompileOrder> myCompileOrder;
    private final CheckBox myDynamics;
    private final CheckBox myPostfixOps;
    private final CheckBox myReflectiveCalls;
    private final CheckBox myImplicitConversions;
    private final CheckBox myHigherKinds;
    private final CheckBox myExistentials;
    private final CheckBox myFeatureWarnings;
    private final CheckBox myMacros;
    private final CheckBox mySpecialization;
    private final CheckBox myExperimental;

    private boolean myNameHashing;
    private boolean myRecompileOnMacroDef;
    private int myTransitiveStep;
    private double myRecompileAllFraction;

    private final MyPathEditor myPluginsEditor = new MyPathEditor(new FileChooserDescriptor(true, false, true, true, false, true));

    @RequiredUIAccess
    public ScalaCompilerSettingsPanel() {
        myCompileOrder = ComboBox.<CompileOrder>builder()
                .add(CompileOrder.Mixed, message("compile.order.mixed"))
                .add(CompileOrder.JavaThenScala, message("compile.order.java.then.scala"))
                .add(CompileOrder.ScalaThenJava, message("compile.order.scala.then.java"))
                .build();

        myDebuggingInfoLevel = ComboBox.<DebuggingInfoLevel>builder()
                .add(DebuggingInfoLevel.None, message("debug.info.level.none"))
                .add(DebuggingInfoLevel.Source, message("debug.info.level.source"))
                .add(DebuggingInfoLevel.Line, message("debug.info.level.source.and.line.number"))
                .add(DebuggingInfoLevel.Vars, message("debug.info.level.source.line.number.and.local.variable"))
                .add(DebuggingInfoLevel.Notailcalls, message("debug.info.level.complete.no.tail.call.optimization"))
                .build();

        myDynamics = checkBox("feature.dynamics", null);
        myPostfixOps = checkBox("feature.postfix.notation", null);
        myReflectiveCalls = checkBox("feature.reflective.calls", null);
        myImplicitConversions = checkBox("feature.implicit.conversions", null);
        myHigherKinds = checkBox("feature.higher.kinded.types", null);
        myExistentials = checkBox("feature.existential.types", null);
        myMacros = checkBox("feature.macros", null);
        myExperimental = checkBox("feature.experimental.features", null);

        myWarnings = checkBox("enable.warnings", "enable.warnings.tooltip");
        myDeprecationWarnings = checkBox("deprecation.warnings", "deprecation.warnings.tooltip");
        myUncheckedWarnings = checkBox("unchecked.warnings", "unchecked.warnings.tooltip");
        myFeatureWarnings = checkBox("feature.warnings", "feature.warnings.tooltip");
        myOptimiseBytecode = checkBox("optimise.bytecode", "optimise.bytecode.tooltip");
        myExplainTypeErrors = checkBox("explain.type.errors", "explain.type.errors.tooltip");
        mySpecialization = checkBox("enable.specialization", "enable.specialization.tooltip");
        myContinuations = checkBox("enable.continuations", null);

        myAdditionalCompilerOptions = TextBoxWithExpandAction.create(PlatformIconGroup.actionsShow(), "", lineSplitter, lineJoiner);

        VerticalLayout options = VerticalLayout.create();
        options.add(columns(
                new CheckBox[]{myWarnings, myDeprecationWarnings, myUncheckedWarnings, myFeatureWarnings},
                new CheckBox[]{myOptimiseBytecode, myExplainTypeErrors, mySpecialization, myContinuations}
        ));
        options.add(LabeledBuilder.simple(message("debugging.info.level"), myDebuggingInfoLevel));
        options.add(LabeledBuilder.filled(message("additional.compiler.options"), myAdditionalCompilerOptions));

        myContentPanel = VerticalLayout.create();
        myContentPanel.add(LabeledBuilder.simple(message("compile.order"), myCompileOrder));
        myContentPanel.add(LabeledLayout.create(message("features"), columns(
                new CheckBox[]{myDynamics, myPostfixOps, myReflectiveCalls, myImplicitConversions},
                new CheckBox[]{myHigherKinds, myExistentials, myMacros, myExperimental}
        )));
        myContentPanel.add(LabeledLayout.create(message("options"), options));
        myContentPanel.add(LabeledLayout.create(message("compiler.plugins"), myPluginsEditor.createUIComponent()));
    }

    private static LocalizeValue message(String key) {
        return LocalizeValue.of(ScalaBundle.message(key));
    }

    @RequiredUIAccess
    private static CheckBox checkBox(String key, String tooltipKey) {
        CheckBox checkBox = CheckBox.create(message(key));
        if (tooltipKey != null) {
            checkBox.setToolTipText(message(tooltipKey));
        }
        return checkBox;
    }

    @RequiredUIAccess
    private static Component columns(CheckBox[] left, CheckBox[] right) {
        VerticalLayout leftColumn = VerticalLayout.create();
        for (CheckBox checkBox : left) {
            leftColumn.add(checkBox);
        }
        VerticalLayout rightColumn = VerticalLayout.create();
        for (CheckBox checkBox : right) {
            rightColumn.add(checkBox);
        }
        HorizontalLayout layout = HorizontalLayout.create();
        layout.add(leftColumn);
        layout.add(rightColumn);
        return layout;
    }

    public ScalaCompilerSettingsState getState() {
        ScalaCompilerSettingsState state = new ScalaCompilerSettingsState();

        state.dynamics = myDynamics.getValue();
        state.postfixOps = myPostfixOps.getValue();
        state.reflectiveCalls = myReflectiveCalls.getValue();
        state.implicitConversions = myImplicitConversions.getValue();
        state.higherKinds = myHigherKinds.getValue();
        state.existentials = myExistentials.getValue();
        state.macros = myMacros.getValue();
        state.experimental = myExperimental.getValue();

        state.compileOrder = myCompileOrder.getValue();
        state.warnings = myWarnings.getValue();
        state.deprecationWarnings = myDeprecationWarnings.getValue();
        state.uncheckedWarnings = myUncheckedWarnings.getValue();
        state.featureWarnings = myFeatureWarnings.getValue();
        state.optimiseBytecode = myOptimiseBytecode.getValue();
        state.explainTypeErrors = myExplainTypeErrors.getValue();
        state.specialization = mySpecialization.getValue();
        state.continuations = myContinuations.getValue();
        state.debuggingInfoLevel = myDebuggingInfoLevel.getValue();
        String rawOptions = myAdditionalCompilerOptions.getValue();
        String options = rawOptions == null ? "" : rawOptions.trim();
        state.additionalCompilerOptions = options.isEmpty() ? new String[0] : lineSplitter.apply(options).toArray(String[]::new);
        state.pluginsClasspath = urlsToPaths(myPluginsEditor.getPaths());

        state.nameHashing = myNameHashing;
        state.recompileOnMacroDef = myRecompileOnMacroDef;
        state.transitiveStep = myTransitiveStep;
        state.recompileAllFraction = myRecompileAllFraction;

        return state;
    }

    private static String[] urlsToPaths(String[] urls) {
        String[] result = new String[urls.length];
        int i = 0;
        for (String url : urls) {
            result[i] = VirtualFileUtil.urlToPath(url);
            i++;
        }
        return result;
    }

    @RequiredUIAccess
    public void setState(ScalaCompilerSettingsState state) {
        myDynamics.setValue(state.dynamics);
        myPostfixOps.setValue(state.postfixOps);
        myReflectiveCalls.setValue(state.reflectiveCalls);
        myImplicitConversions.setValue(state.implicitConversions);
        myHigherKinds.setValue(state.higherKinds);
        myExistentials.setValue(state.existentials);
        myMacros.setValue(state.macros);
        myExperimental.setValue(state.experimental);

        myCompileOrder.setValue(state.compileOrder);
        myWarnings.setValue(state.warnings);
        myDeprecationWarnings.setValue(state.deprecationWarnings);
        myUncheckedWarnings.setValue(state.uncheckedWarnings);
        myFeatureWarnings.setValue(state.featureWarnings);
        myOptimiseBytecode.setValue(state.optimiseBytecode);
        myExplainTypeErrors.setValue(state.explainTypeErrors);
        mySpecialization.setValue(state.specialization);
        myContinuations.setValue(state.continuations);
        myDebuggingInfoLevel.setValue(state.debuggingInfoLevel);
        myAdditionalCompilerOptions.setValue(lineJoiner.apply(Arrays.asList(state.additionalCompilerOptions)));
        myPluginsEditor.setPaths(pathsToUrls(state.pluginsClasspath));

        myNameHashing = state.nameHashing;
        myRecompileOnMacroDef = state.recompileOnMacroDef;
        myTransitiveStep = state.transitiveStep;
        myRecompileAllFraction = state.recompileAllFraction;
    }

    private static String[] pathsToUrls(String[] paths) {
        String[] result = new String[paths.length];
        int i = 0;
        for (String path : paths) {
            result[i] = VirtualFileUtil.pathToUrl(path);
            i++;
        }
        return result;
    }

    public Component getComponent() {
        return myContentPanel;
    }

    public void saveTo(ScalaCompilerSettingsProfile profile) {
        profile.setSettings(ScalaCompilerSettings.fromState(getState()));
    }

    @RequiredUIAccess
    public void setProfile(ScalaCompilerSettingsProfile profile) {
        setState(profile.getSettings().toState());
    }

    // A space (' ') followed by two backslashes ('\') followed by a space.
    // This separator is used to join all options on a single line when the editor is collapsed to a single line.
    // It's not shown when the whole list of options is expanded.
    // The separator was chosen such that it probably won't show up in any settings and cause them to be split when
    // they shouldn't.
    private static final String joinSeparator = " \\\\ ";

    // Same as the `joinSeparator` above, but in regex form, with extra escape characters.
    @Language("RegExp")
    private static final String splitRegex = " \\\\\\\\ ";

    private static final Function<String, List<String>> lineSplitter =
            string -> Arrays.asList(string.split(splitRegex));

    private static final Function<List<String>, String> lineJoiner =
            strings -> String.join(joinSeparator, strings);
}
