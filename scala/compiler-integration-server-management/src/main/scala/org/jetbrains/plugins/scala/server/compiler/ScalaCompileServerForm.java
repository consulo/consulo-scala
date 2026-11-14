package org.jetbrains.plugins.scala.server.compiler;

import consulo.util.lang.StringUtil;

import com.intellij.java.language.projectRoots.JavaSdkType;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.disposer.Disposable;
import consulo.java.language.bundle.JavaSdkTypeUtil;
import consulo.localize.LocalizeValue;
import consulo.module.ui.BundleBox;
import consulo.module.ui.BundleBoxBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.LabeledBuilder;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider$;
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfigurable$;
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings;
import scala.Some;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.Optional;

@ExtensionImpl
public class ScalaCompileServerForm implements ProjectConfigurable, SearchableConfigurable {
    private VerticalLayout myContentPanel;
    private VerticalLayout myJvmSettingsPanel;
    private VerticalLayout myAdvancedSettingsPanel;
    private TextBoxWithExpandAction myCompilationServerJvmParameters;
    private TextBox myCompilationServerMaximumHeapSize;
    private CheckBox myEnableCompileServer;
    private BundleBox myCompilationServerSdk;
    private CheckBox myProjectHomeChb;
    private CheckBox myShutdownServerCheckBox;
    private IntBox myShutdownDelay;
    private IntBox myParallelism;
    private CheckBox myParallelCompilation;
    private HtmlLabel myJdkWarningLabel;
    private final ScalaCompileServerSettings mySettings;
    private final Project myProject;

    // upstream: ProjectSdksModelWithDefault adds a "Recommended JDK" ProjectJdkImpl to the JdkComboBox model
    private static final String DEFAULT_SDK_ITEM = "#ScalaCompileServerRecommendedJdk";

    public static final String ID = "scala.compile.server";

    public static final class SearchFilter {
        // TODO: will not work with localized IDEA
        public static String USE_COMPILE_SERVER_FOR_SCALA = "use scala compile server";
    }

    @Inject
    public ScalaCompileServerForm(Project project) {
        myProject = project;
        mySettings = ScalaCompileServerSettings.getInstance();
    }

    private static LocalizeValue message(String key) {
        return LocalizeValue.of(ServerManagementBundle.message(key));
    }

    @RequiredUIAccess
    private void createUI(Disposable parentDisposable) {
        myEnableCompileServer = CheckBox.create(message("compile.server.use.for.scala"));

        myParallelCompilation = CheckBox.create(message("compile.server.parallel.compilation"));
        myParallelism = IntBox.create(Math.max(1, mySettings.COMPILE_SERVER_PARALLELISM)).withRange(1, 9999);
        HorizontalLayout parallelismSettings = HorizontalLayout.create();
        parallelismSettings.add(myParallelism);
        parallelismSettings.add(Label.create(message("compile.server.parallelism")));

        myShutdownServerCheckBox = CheckBox.create(message("compile.server.shutdown.if.idle.for"));
        myShutdownDelay = IntBox.create(Math.max(0, mySettings.COMPILE_SERVER_SHUTDOWN_DELAY)).withRange(0, 24 * 60);
        HorizontalLayout shutdownSettings = HorizontalLayout.create();
        shutdownSettings.add(myShutdownDelay);
        shutdownSettings.add(Label.create(message("minutes")));

        myProjectHomeChb = CheckBox.create(message("compile.server.use.project.home"));
        // upstream: UI.PanelFactory.panel(...).withTooltip(...) - a context help label
        myProjectHomeChb.setToolTipText(message("compile.server.new.project.restart"));

        myAdvancedSettingsPanel = VerticalLayout.create();
        myAdvancedSettingsPanel.add(HorizontalLayout.create().add(myParallelCompilation).add(parallelismSettings));
        myAdvancedSettingsPanel.add(HorizontalLayout.create().add(myShutdownServerCheckBox).add(shutdownSettings));
        myAdvancedSettingsPanel.add(myProjectHomeChb);

        // upstream: JdkComboBox with an "add JDK" action (SdkUtils.addJdkIfNotExists)
        myCompilationServerSdk = BundleBoxBuilder.create(parentDisposable)
                .withSdkTypeFilter(sdkTypeId -> sdkTypeId instanceof JavaSdkType)
                .build();
        Sdk defaultSdk = CompileServerLauncher.defaultSdk(myProject);
        myCompilationServerSdk.addCustomBundleItem(
            DEFAULT_SDK_ITEM,
            "Recommended JDK (" + defaultSdk.getName() + ")",
            JavaSdkType.getDefaultJavaSdkType().getIcon()
        );
        myCompilationServerSdk.getComponent().setToolTipText(message("compile.server.description"));

        myJdkWarningLabel = HtmlLabel.create(LocalizeValue.empty());
        myJdkWarningLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        myJdkWarningLabel.setVisible(false);
        myCompilationServerSdk.getComponent().addValueListener(e -> updateJdkWarning());

        myCompilationServerMaximumHeapSize = TextBox.create().withVisibleLength(5);
        // Accept only positive values; the field can be empty, in which case the -Xmx VM option will not be generated.
        myCompilationServerMaximumHeapSize.addValidator(value ->
                value == null || value.isBlank() || parseHeapSize(value) > 0 ? null : new TextBox.ValidationInfo(""));

        myCompilationServerJvmParameters = TextBoxWithExpandAction.create(
                PlatformIconGroup.actionsShow(),
                ServerManagementBundle.message("compile.server.jvm.command.line.parameters"),
                ParametersListUtil.DEFAULT_LINE_PARSER,
                ParametersListUtil.DEFAULT_LINE_JOINER
        );

        myJvmSettingsPanel = VerticalLayout.create();
        myJvmSettingsPanel.add(LabeledBuilder.filled(message("jdk"), myCompilationServerSdk));
        myJvmSettingsPanel.add(myJdkWarningLabel);
        myJvmSettingsPanel.add(LabeledBuilder.simple(message("jvm.maximum.heap.size.mb"), myCompilationServerMaximumHeapSize));
        myJvmSettingsPanel.add(LabeledBuilder.filled(message("jvm.options"), myCompilationServerJvmParameters));

        myContentPanel = VerticalLayout.create();
        myContentPanel.add(myEnableCompileServer);
        myContentPanel.add(myAdvancedSettingsPanel);
        myContentPanel.add(LabeledLayout.create(LocalizeValue.of("JVM"), myJvmSettingsPanel));

        myEnableCompileServer.addValueListener(e -> updateJvmSettingsPanel());
        myParallelCompilation.addValueListener(e -> updateParallelCompilationSettingsPanel());
        myShutdownServerCheckBox.addValueListener(e -> updateShutdownSettingsPanel());
        parallelismSettingsPanel = parallelismSettings;
        shutdownSettingsPanel = shutdownSettings;
    }

    private HorizontalLayout parallelismSettingsPanel;
    private HorizontalLayout shutdownSettingsPanel;

    @RequiredUIAccess
    private void updateJdkWarning() {
        Optional.ofNullable(getSelectedSdk())
                .map(JavaSdkTypeUtil::getVersion)
                .ifPresent(version -> {
                    if (!CompileServerJdkManager$.MODULE$.isCompatible(version)) {
                        myJdkWarningLabel.setText(message("compile.server.jdk.too.old"));
                        myJdkWarningLabel.setVisible(true);
                    } else if (CompileServerJdkManager$.MODULE$.isRecommendedVersionForProject(myProject, version)) {
                        myJdkWarningLabel.setVisible(false);
                    } else {
                        myJdkWarningLabel.setText(message("compile.server.wrong.jdk.selected"));
                        myJdkWarningLabel.setVisible(true);
                    }
                });
    }

    @RequiredUIAccess
    private void updateJvmSettingsPanel() {
        boolean compileServerEnabled = myEnableCompileServer.getValue();
        myJvmSettingsPanel.setEnabledRecursive(compileServerEnabled);
        myAdvancedSettingsPanel.setEnabledRecursive(compileServerEnabled);
        if (compileServerEnabled) {
            updateParallelCompilationSettingsPanel();
            updateShutdownSettingsPanel();
        }
    }

    @RequiredUIAccess
    private void updateParallelCompilationSettingsPanel() {
        parallelismSettingsPanel.setEnabledRecursive(myParallelCompilation.getValue());
    }

    @RequiredUIAccess
    private void updateShutdownSettingsPanel() {
        shutdownSettingsPanel.setEnabledRecursive(myShutdownServerCheckBox.getValue());
    }

    private boolean isDefaultSdkSelected() {
        return DEFAULT_SDK_ITEM.equals(myCompilationServerSdk.getSelectedBundleName());
    }

    @Nullable
    private Sdk getSelectedSdk() {
        if (isDefaultSdkSelected()) {
            return CompileServerLauncher.defaultSdk(myProject);
        }
        String name = myCompilationServerSdk.getSelectedBundleName();
        return name == null ? null : SdkTable.getInstance().findSdk(name);
    }

    private static int parseHeapSize(String text) {
        try {
            return Integer.parseInt(text.trim());
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Serializes the maximum heap size text field value to a string.
     *
     * @return the heap size as string if valid, otherwise an empty string
     */
    private String heapSizeAsString() {
        String text = StringUtil.notNullize(myCompilationServerMaximumHeapSize.getValue());
        final var size = text == null ? -1 : parseHeapSize(text);
        return size > 0 ? Integer.toString(size) : "";
    }

    private static String notNull(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getParentId() {
        return ScalaCompilerConfigurable$.MODULE$.Id();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(ServerManagementBundle.message("displayname.scala.compile.server"));
    }

    @Override
    @Nullable
    public String getHelpTopic() {
        return ScalaWebHelpProvider$.MODULE$.HelpPrefix() + "compile-and-build-scala-projects.html#scala_compilation";
    }

    @Override
    @RequiredUIAccess
    public Component createUIComponent(Disposable parentDisposable) {
        if (myContentPanel == null) {
            createUI(parentDisposable);
        }
        return myContentPanel;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        if (myContentPanel == null) {
            return false;
        }

        final var sdkName = myCompilationServerSdk.getSelectedBundleName();

        final var compileServerEnabledMatches = myEnableCompileServer.getValue() == mySettings.COMPILE_SERVER_ENABLED;
        final var sdkSettingsMatch = isDefaultSdkSelected()
                ? mySettings.USE_DEFAULT_SDK
                : !mySettings.USE_DEFAULT_SDK && Objects.equals(sdkName, mySettings.COMPILE_SERVER_SDK);
        final var heapSizeMatches = heapSizeAsString().equals(mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE);
        final var serverParamsMatch = notNull(myCompilationServerJvmParameters.getValue()).equals(mySettings.COMPILE_SERVER_JVM_PARAMETERS);
        final var useProjectDirMatches = myProjectHomeChb.getValue() == mySettings.USE_PROJECT_HOME_AS_WORKING_DIR;
        final var shutdownIdleMatches = myShutdownServerCheckBox.getValue() == mySettings.COMPILE_SERVER_SHUTDOWN_IDLE;
        final var shutdownDelayMatches = myShutdownDelay.getValueOrError() == mySettings.COMPILE_SERVER_SHUTDOWN_DELAY;
        final var parallelismMatches = myParallelism.getValueOrError() == mySettings.COMPILE_SERVER_PARALLELISM;
        final var parallelCompilationMatches = myParallelCompilation.getValue() == mySettings.COMPILE_SERVER_PARALLEL_COMPILATION;

        return !(
                compileServerEnabledMatches &&
                sdkSettingsMatch &&
                heapSizeMatches &&
                serverParamsMatch &&
                useProjectDirMatches &&
                shutdownIdleMatches &&
                shutdownDelayMatches &&
                parallelismMatches &&
                parallelCompilationMatches
        );
    }

    @Override
    @RequiredUIAccess
    public void apply() {
        if (myContentPanel == null) {
            return;
        }

        mySettings.COMPILE_SERVER_ENABLED = myEnableCompileServer.getValue();
        if (!mySettings.COMPILE_SERVER_ENABLED) {
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    CompileServerLauncher.stopServerAndWaitFor(Duration.Zero(), Some.apply("compile server disabled from settings"))
            );
        }

        Sdk sdk = getSelectedSdk();
        mySettings.USE_DEFAULT_SDK = isDefaultSdkSelected();
        mySettings.COMPILE_SERVER_SDK = sdk == null ? null : sdk.getName();

        mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE = heapSizeAsString();
        mySettings.COMPILE_SERVER_JVM_PARAMETERS = notNull(myCompilationServerJvmParameters.getValue());

        mySettings.USE_PROJECT_HOME_AS_WORKING_DIR = myProjectHomeChb.getValue();

        mySettings.COMPILE_SERVER_SHUTDOWN_IDLE = myShutdownServerCheckBox.getValue();
        mySettings.COMPILE_SERVER_SHUTDOWN_DELAY = myShutdownDelay.getValueOrError();

        // upstream: BuildManager.getInstance().clearState() when parallelism settings change (cancels preloaded JPS builds, SCL-18512)
        mySettings.COMPILE_SERVER_PARALLELISM = myParallelism.getValueOrError();
        mySettings.COMPILE_SERVER_PARALLEL_COMPILATION = myParallelCompilation.getValue();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        if (myContentPanel == null) {
            return;
        }

        myEnableCompileServer.setValue(mySettings.COMPILE_SERVER_ENABLED);

        if (mySettings.USE_DEFAULT_SDK) {
            myCompilationServerSdk.setSelectedBundle(DEFAULT_SDK_ITEM);
        }
        else {
            myCompilationServerSdk.setSelectedBundle(mySettings.COMPILE_SERVER_SDK);
        }

        myCompilationServerMaximumHeapSize.setValue(notNull(mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE));
        myCompilationServerJvmParameters.setValue(notNull(mySettings.COMPILE_SERVER_JVM_PARAMETERS));

        myShutdownServerCheckBox.setValue(mySettings.COMPILE_SERVER_SHUTDOWN_IDLE);
        myShutdownDelay.setValue(mySettings.COMPILE_SERVER_SHUTDOWN_DELAY);
        myParallelism.setValue(mySettings.COMPILE_SERVER_PARALLELISM);
        myParallelCompilation.setValue(mySettings.COMPILE_SERVER_PARALLEL_COMPILATION);

        myProjectHomeChb.setValue(mySettings.USE_PROJECT_HOME_AS_WORKING_DIR);

        updateJvmSettingsPanel();
        updateJdkWarning();
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        myContentPanel = null;
    }
}
