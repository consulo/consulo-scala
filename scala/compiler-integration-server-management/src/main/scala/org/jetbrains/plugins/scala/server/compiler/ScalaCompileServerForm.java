package org.jetbrains.plugins.scala.server.compiler;

import com.intellij.java.language.projectRoots.JavaSdkType;
import com.intellij.java.language.projectRoots.JavaSdkVersion;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.disposer.Disposable;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.java.language.bundle.JavaSdkTypeUtil;
import consulo.localize.LocalizeValue;
import consulo.module.ui.awt.SdkComboBox;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.IntegerField;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider$;
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfigurable$;
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings;
import scala.Some;
import scala.concurrent.duration.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

@SuppressWarnings("deprecation")
@ExtensionImpl
public class ScalaCompileServerForm implements ProjectConfigurable, SearchableConfigurable {
    private JPanel myJvmSettingsPanel;
    private RawCommandLineEditor myCompilationServerJvmParameters;
    private IntegerField myCompilationServerMaximumHeapSize;
    private JCheckBox myEnableCompileServer;
    private JPanel myContentPanel;
    private final SdkComboBox myCompilationServerSdk;
    private JPanel mySdkPanel;
    private JCheckBox myProjectHomeChb;
    private JCheckBox myShutdownServerCheckBox;
    private JSpinner myShutdownDelay;
    private JSpinner myParallelism;
    private JCheckBox myParallelCompilation;
    private JPanel myParallelCompilationSettingsPanel;
    private JPanel myShutdownSettingsPanel;
    private JPanel myAdvancedSettingsPanel;
    private JPanel myUseProjectHomePanel;
    private TitledSeparator myJvmTitle;
    private JLabel myJdkWarningLabel;
    private final ScalaCompileServerSettings mySettings;
    private final Project myProject;

    private static final int HEAP_SIZE_NOT_SET = -1;

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

        // Accept only positive values.
        myCompilationServerMaximumHeapSize.setMinValue(1);
        // If the user provides an incorrect (or empty) value, `HEAP_SIZE_NOT_SET` is returned instead as a sentinel value.
        myCompilationServerMaximumHeapSize.setDefaultValue(HEAP_SIZE_NOT_SET);
        // Allow the field to be empty, in which case the -Xmx VM option will not be generated.
        myCompilationServerMaximumHeapSize.setCanBeEmpty(true);

        myProjectHomeChb = new JCheckBox(ServerManagementBundle.message("compile.server.use.project.home"));
        // upstream: UI.PanelFactory.panel(...).withTooltip(...) - a context help label
        myProjectHomeChb.setToolTipText(ServerManagementBundle.message("compile.server.new.project.restart"));
        myUseProjectHomePanel.add(myProjectHomeChb);

        myEnableCompileServer.addChangeListener(e -> updateJvmSettingsPanel());
        myParallelCompilation.addChangeListener(e -> updateParallelCompilationSettingsPanel());
        myShutdownServerCheckBox.addChangeListener(e -> updateShutdownSettingsPanel());

        // upstream: JdkComboBox with an "add JDK" action (SdkUtils.addJdkIfNotExists)
        myCompilationServerSdk = new SdkComboBox(SdkTable.getInstance(), sdkTypeId -> sdkTypeId instanceof JavaSdkType, false);
        Sdk defaultSdk = CompileServerLauncher.defaultSdk(project);
        myCompilationServerSdk.insertCustomSdkItem(
            DEFAULT_SDK_ITEM,
            "Recommended JDK (" + defaultSdk.getName() + ")",
            JavaSdkType.getDefaultJavaSdkType().getIcon()
        );
        myCompilationServerSdk.setMinimumSize(new Dimension(500, myCompilationServerSdk.getMinimumSize().height));
        myCompilationServerSdk.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Optional.ofNullable(getSelectedSdk())
                        .map(JavaSdkTypeUtil::getVersion)
                        .ifPresent(version -> {
                            if (!CompileServerJdkManager$.MODULE$.isCompatible(version)) {
                                myJdkWarningLabel.setForeground(JBColor.RED);
                                final var text = ServerManagementBundle.message("compile.server.jdk.too.old");
                                myJdkWarningLabel.setText("<html>" + text + "</html>");
                                myJdkWarningLabel.setVisible(true);
                            } else if (CompileServerJdkManager$.MODULE$.isRecommendedVersionForProject(project, version)) {
                                myJdkWarningLabel.setVisible(false);
                            } else {
                                myJdkWarningLabel.setForeground(JBColor.RED);
                                final var text = ServerManagementBundle.message("compile.server.wrong.jdk.selected");
                                myJdkWarningLabel.setText("<html>" + text + "</html>");
                                myJdkWarningLabel.setVisible(true);
                            }
                        });
            }
        });

        myCompilationServerSdk.setToolTipText(ServerManagementBundle.message("compile.server.description"));
        mySdkPanel.add(myCompilationServerSdk, BorderLayout.CENTER);

        myShutdownDelay.setModel(new SpinnerNumberModel(mySettings.COMPILE_SERVER_SHUTDOWN_DELAY, 0, 24 * 60, 1));
        myParallelism.setModel(new SpinnerNumberModel(mySettings.COMPILE_SERVER_PARALLELISM, 1, 9999, 1));

        updateJvmSettingsPanel();
    }

    private void updateJvmSettingsPanel() {
        boolean compileServerEnabled = myEnableCompileServer.isSelected();
        setDescendantsEnabledIn(myJvmSettingsPanel, compileServerEnabled);
        setDescendantsEnabledIn(myAdvancedSettingsPanel, compileServerEnabled);
        myJvmTitle.setEnabled(compileServerEnabled);
    }

    private void updateParallelCompilationSettingsPanel() {
        setDescendantsEnabledIn(myParallelCompilationSettingsPanel, myParallelCompilation.isSelected());
    }

    private void updateShutdownSettingsPanel() {
        setDescendantsEnabledIn(myShutdownSettingsPanel, myShutdownServerCheckBox.isSelected());
    }

    private boolean isDefaultSdkSelected() {
        return DEFAULT_SDK_ITEM.equals(myCompilationServerSdk.getSelectedSdkName());
    }

    @Nullable
    private Sdk getSelectedSdk() {
        return isDefaultSdkSelected() ? CompileServerLauncher.defaultSdk(myProject) : myCompilationServerSdk.getSelectedSdk();
    }

    private static void setDescendantsEnabledIn(JComponent root, boolean b) {
        for (java.awt.Component child : root.getComponents()) {
            child.setEnabled(b);
            if (child instanceof JComponent) {
                setDescendantsEnabledIn((JComponent) child, b);
            }
        }
    }

    /**
     * Serializes the maximum heap size text field value to a string.
     *
     * @return the heap size as string if valid, otherwise an empty string if it matches the sentinel value
     */
    private String heapSizeAsString() {
        final var size = myCompilationServerMaximumHeapSize.getValue();
        return size == HEAP_SIZE_NOT_SET ? "" : size.toString();
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
    @Nullable
    public Component createUIComponent(Disposable parentDisposable) {
        return TargetAWT.wrap(myContentPanel);
    }

    @Override
    public boolean isModified() {
        final var sdkName = myCompilationServerSdk.getSelectedSdkName();

        final var compileServerEnabledMatches = myEnableCompileServer.isSelected() == mySettings.COMPILE_SERVER_ENABLED;
        final var sdkSettingsMatch = isDefaultSdkSelected()
                ? mySettings.USE_DEFAULT_SDK
                : !mySettings.USE_DEFAULT_SDK && Objects.equals(sdkName, mySettings.COMPILE_SERVER_SDK);
        final var heapSizeMatches = heapSizeAsString().equals(mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE);
        final var serverParamsMatch = myCompilationServerJvmParameters.getText().equals(mySettings.COMPILE_SERVER_JVM_PARAMETERS);
        final var useProjectDirMatches = myProjectHomeChb.isSelected() == mySettings.USE_PROJECT_HOME_AS_WORKING_DIR;
        final var shutdownIdleMatches = myShutdownServerCheckBox.isSelected() == mySettings.COMPILE_SERVER_SHUTDOWN_IDLE;
        final var shutdownDelayMatches = (Integer) (myShutdownDelay.getModel().getValue()) == mySettings.COMPILE_SERVER_SHUTDOWN_DELAY;
        final var parallelismMatches = (Integer) (myParallelism.getModel().getValue()) == mySettings.COMPILE_SERVER_PARALLELISM;
        final var parallelCompilationMatches = myParallelCompilation.isSelected() == mySettings.COMPILE_SERVER_PARALLEL_COMPILATION;

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
    public void apply() {
        mySettings.COMPILE_SERVER_ENABLED = myEnableCompileServer.isSelected();
        if (!mySettings.COMPILE_SERVER_ENABLED) {
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    CompileServerLauncher.stopServerAndWaitFor(Duration.Zero(), Some.apply("compile server disabled from settings"))
            );
        }

        Sdk sdk = getSelectedSdk();
        mySettings.USE_DEFAULT_SDK = isDefaultSdkSelected();
        mySettings.COMPILE_SERVER_SDK = sdk == null ? null : sdk.getName();

        mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE = heapSizeAsString();
        mySettings.COMPILE_SERVER_JVM_PARAMETERS = myCompilationServerJvmParameters.getText();

        mySettings.USE_PROJECT_HOME_AS_WORKING_DIR = myProjectHomeChb.isSelected();

        mySettings.COMPILE_SERVER_SHUTDOWN_IDLE = myShutdownServerCheckBox.isSelected();
        mySettings.COMPILE_SERVER_SHUTDOWN_DELAY = (Integer) (myShutdownDelay.getModel().getValue());

        Integer newParallelism = (Integer) (myParallelism.getModel().getValue());
        Boolean newParallelCompilation = myParallelCompilation.isSelected();
        // upstream: BuildManager.getInstance().clearState() when parallelism settings change (cancels preloaded JPS builds, SCL-18512)
        mySettings.COMPILE_SERVER_PARALLELISM = newParallelism;
        mySettings.COMPILE_SERVER_PARALLEL_COMPILATION = newParallelCompilation;
    }

    @Override
    public void reset() {
        myEnableCompileServer.setSelected(mySettings.COMPILE_SERVER_ENABLED);

        if (mySettings.USE_DEFAULT_SDK) {
            myCompilationServerSdk.setSelectedSdk(DEFAULT_SDK_ITEM);
        }
        else {
            myCompilationServerSdk.setSelectedSdk(mySettings.COMPILE_SERVER_SDK);
        }

        myCompilationServerMaximumHeapSize.setText(mySettings.COMPILE_SERVER_MAXIMUM_HEAP_SIZE);
        myCompilationServerJvmParameters.setText(mySettings.COMPILE_SERVER_JVM_PARAMETERS);

        myShutdownServerCheckBox.setSelected(mySettings.COMPILE_SERVER_SHUTDOWN_IDLE);
        myShutdownDelay.getModel().setValue(mySettings.COMPILE_SERVER_SHUTDOWN_DELAY);
        myParallelism.getModel().setValue(mySettings.COMPILE_SERVER_PARALLELISM);
        myParallelCompilation.setSelected(mySettings.COMPILE_SERVER_PARALLEL_COMPILATION);

        myProjectHomeChb.setSelected(mySettings.USE_PROJECT_HOME_AS_WORKING_DIR);
    }

    @Override
    public void disposeUIResources() {
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        myContentPanel = new JPanel();
        myContentPanel.setLayout(new GridLayoutManager(11, 4, new Insets(0, 0, 0, 0), -1, -1));
        myAdvancedSettingsPanel = new JPanel();
        myAdvancedSettingsPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        myContentPanel.add(myAdvancedSettingsPanel, new GridConstraints(1, 0, 7, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        myAdvancedSettingsPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myShutdownServerCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myShutdownServerCheckBox, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "compile.server.shutdown.if.idle.for"));
        panel1.add(myShutdownServerCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myShutdownSettingsPanel = new JPanel();
        myShutdownSettingsPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(myShutdownSettingsPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myShutdownDelay = new JSpinner();
        myShutdownSettingsPanel.add(myShutdownDelay, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "minutes"));
        myShutdownSettingsPanel.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        myShutdownSettingsPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myUseProjectHomePanel = new JPanel();
        myUseProjectHomePanel.setLayout(new BorderLayout(0, 0));
        myAdvancedSettingsPanel.add(myUseProjectHomePanel, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myAdvancedSettingsPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myParallelCompilation = new JCheckBox();
        this.$$$loadButtonText$$$(myParallelCompilation, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "compile.server.parallel.compilation"));
        panel2.add(myParallelCompilation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myParallelCompilationSettingsPanel = new JPanel();
        myParallelCompilationSettingsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(myParallelCompilationSettingsPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myParallelism = new JSpinner();
        myParallelCompilationSettingsPanel.add(myParallelism, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "compile.server.parallelism"));
        myParallelCompilationSettingsPanel.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        myContentPanel.add(spacer3, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myJvmSettingsPanel = new JPanel();
        myJvmSettingsPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        myContentPanel.add(myJvmSettingsPanel, new GridConstraints(9, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label3 = new JLabel();
        label3.setEnabled(true);
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "jvm.options"));
        myJvmSettingsPanel.add(label3, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCompilationServerJvmParameters = new RawCommandLineEditor();
        myCompilationServerJvmParameters.setDialogCaption(this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "compile.server.jvm.command.line.parameters"));
        myCompilationServerJvmParameters.setEnabled(true);
        myJvmSettingsPanel.add(myCompilationServerJvmParameters, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(250, -1), new Dimension(500, 27), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setEnabled(true);
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "jvm.maximum.heap.size.mb"));
        myJvmSettingsPanel.add(label4, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCompilationServerMaximumHeapSize = new IntegerField();
        myCompilationServerMaximumHeapSize.setColumns(5);
        myCompilationServerMaximumHeapSize.setEnabled(true);
        myJvmSettingsPanel.add(myCompilationServerMaximumHeapSize, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "jdk"));
        myJvmSettingsPanel.add(label5, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySdkPanel = new JPanel();
        mySdkPanel.setLayout(new BorderLayout(0, 0));
        mySdkPanel.setEnabled(false);
        myJvmSettingsPanel.add(mySdkPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myJdkWarningLabel = new JLabel();
        myJdkWarningLabel.setFocusable(true);
        myJdkWarningLabel.setText("");
        myJdkWarningLabel.setVisible(false);
        myJvmSettingsPanel.add(myJdkWarningLabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(500, -1), 1, false));
        myEnableCompileServer = new JCheckBox();
        this.$$$loadButtonText$$$(myEnableCompileServer, this.$$$getMessageFromBundle$$$("org/jetbrains/plugins/scala/server/compiler/ServerManagementBundle", "compile.server.use.for.scala"));
        myContentPanel.add(myEnableCompileServer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myJvmTitle = new TitledSeparator();
        myJvmTitle.setText("JVM");
        myContentPanel.add(myJvmTitle, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    /**
     * @noinspection ALL
     */
    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("consulo.component.util.localize.AbstractBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myContentPanel;
    }

}
