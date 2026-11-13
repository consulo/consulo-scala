package org.jetbrains.plugins.scala.settings.sections;

import consulo.application.AllIcons;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ui.awt.ChooseModulesDialog;
import consulo.project.Project;
import consulo.ui.ex.awt.ItemRemovable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.plugins.scala.util.ui.ContextHelpLabel;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class BasePackagesSettingsSectionPanel extends SettingsSectionPanel {
    private JRadioButton myInheritBasePackagesRadioButton;
    private JRadioButton myUseCustomBasePackagesRadioButton;
    private JPanel myBasePackagesPanel;
    private JPanel myBasePackagesHelpPanel;
    private JPanel rootPanel;
    private final JBTable myBasePackagesTable;

    public BasePackagesSettingsSectionPanel(Project project) {
        super(project);

        myBasePackagesHelpPanel.add(ContextHelpLabel.create(ScalaBundle.message("base.package.help")));

        myBasePackagesTable = new JBTable(new ModuleOptionsTableModel());
        myBasePackagesTable.setRowHeight(JBUIScale.scale(22));

        TableColumn moduleColumn = myBasePackagesTable.getColumnModel().getColumn(0);
        moduleColumn.setHeaderValue(LocalizeValue.localizeTODO("Module").get());
        moduleColumn.setCellRenderer(new ModuleTableCellRenderer());
        int width = myBasePackagesTable.getFontMetrics(myBasePackagesTable.getFont()).stringWidth("Module Title") * 3;
        moduleColumn.setMinWidth(width);
        moduleColumn.setPreferredWidth(width);
        moduleColumn.setMaxWidth(width);

        TableColumn targetLevelColumn = myBasePackagesTable.getColumnModel().getColumn(1);
        targetLevelColumn.setHeaderValue(ScalaBundle.message("scala.project.settings.form.tabs.base.package"));

        new TableSpeedSearch(myBasePackagesTable);

        JPanel tableComp = ToolbarDecorator.createDecorator(myBasePackagesTable)
                .disableUpAction()
                .disableDownAction()
                .setAddAction(b -> addBasePackageModules())
                .setRemoveAction(b -> removeSelectedBasePackageModules())
                .createPanel();
        tableComp.setPreferredSize(new Dimension(myBasePackagesTable.getWidth(), 150));
        myBasePackagesPanel.add(tableComp);

        myInheritBasePackagesRadioButton.addActionListener(actionEvent -> myBasePackagesTable.setEnabled(!myInheritBasePackagesRadioButton.isSelected()));
        myUseCustomBasePackagesRadioButton.addActionListener(actionEvent -> myBasePackagesTable.setEnabled(myUseCustomBasePackagesRadioButton.isSelected()));

        reset();
    }

    @Override
    JComponent getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.isInheritBasePackages() != myInheritBasePackagesRadioButton.isSelected() ||
                !scalaProjectSettings.getCustomBasePackages().equals(getCustomBasePackages());
    }

    @Override
    void apply() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setInheritBasePackages(myInheritBasePackagesRadioButton.isSelected());
        scalaProjectSettings.setCustomBasePackages(getCustomBasePackages());
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        myInheritBasePackagesRadioButton.setSelected(scalaProjectSettings.isInheritBasePackages());
        myUseCustomBasePackagesRadioButton.setSelected(!scalaProjectSettings.isInheritBasePackages());
        myBasePackagesTable.setEnabled(!scalaProjectSettings.isInheritBasePackages());
        setCustomBasePackages(scalaProjectSettings.getCustomBasePackages());
    }


    private Map<String, String> getCustomBasePackages() {
        return ((ModuleOptionsTableModel) myBasePackagesTable.getModel()).getModuleOptions();
    }

    private void setCustomBasePackages(Map<String, String> basePackages) {
        ((ModuleOptionsTableModel) myBasePackagesTable.getModel()).setModuleOptions(myProject, basePackages);
    }

    private void addBasePackageModules() {
        int i = ((ModuleOptionsTableModel) myBasePackagesTable.getModel()).addModulesToModel(myProject, myBasePackagesPanel);
        if (i != -1) {
            TableUtil.selectRows(myBasePackagesTable, new int[]{i});
            TableUtil.scrollSelectionToVisible(myBasePackagesTable);
        }
    }

    private void removeSelectedBasePackageModules() {
        if (myBasePackagesTable.getSelectedRows().length > 0) {
            TableUtil.removeSelectedItems(myBasePackagesTable);
        }
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
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(4, 3, new Insets(9, 9, 9, 9), -1, -1));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myInheritBasePackagesRadioButton = new JRadioButton();
        this.$$$loadButtonText$$$(myInheritBasePackagesRadioButton, this.$$$getMessageFromBundle$$$("messages/ScalaBundle", "scala.project.settings.form.base.package.inherit"));
        rootPanel.add(myInheritBasePackagesRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myUseCustomBasePackagesRadioButton = new JRadioButton();
        this.$$$loadButtonText$$$(myUseCustomBasePackagesRadioButton, this.$$$getMessageFromBundle$$$("messages/ScalaBundle", "scala.project.settings.form.base.package.custom"));
        rootPanel.add(myUseCustomBasePackagesRadioButton, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myBasePackagesPanel = new JPanel();
        myBasePackagesPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myBasePackagesPanel, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myBasePackagesHelpPanel = new JPanel();
        myBasePackagesHelpPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myBasePackagesHelpPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(myUseCustomBasePackagesRadioButton);
        buttonGroup.add(myInheritBasePackagesRadioButton);
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
        return rootPanel;
    }


    // upstream: com.intellij.compiler.options.ModuleOptionsTableModel (IntelliJ java-compiler-impl)
    private static class ModuleOptionsTableModel extends AbstractTableModel implements ItemRemovable {
        private static class Item {
            private final Module module;
            private String option;

            Item(Module module, String option) {
                this.module = module;
                this.option = option;
            }
        }

        private final List<Item> myItems = new ArrayList<>();

        Map<String, String> getModuleOptions() {
            Map<String, String> map = new HashMap<>();
            for (Item item : myItems) {
                map.put(item.module.getName(), item.option);
            }
            return map;
        }

        void setModuleOptions(Project project, Map<String, String> moduleOptions) {
            myItems.clear();
            if (!moduleOptions.isEmpty()) {
                for (Module module : ModuleManager.getInstance(project).getModules()) {
                    String options = moduleOptions.get(module.getName());
                    if (options != null) {
                        myItems.add(new Item(module, options));
                    }
                }
                sortItems();
            }
            fireTableDataChanged();
        }

        int addModulesToModel(Project project, Component parent) {
            Set<Module> existing = new HashSet<>();
            for (Item item : myItems) {
                existing.add(item.module);
            }

            List<Module> candidates = new ArrayList<>();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                if (!existing.contains(module)) {
                    candidates.add(module);
                }
            }

            if (!candidates.isEmpty()) {
                candidates.sort(Comparator.comparing(Module::getName));
                ChooseModulesDialog chooser = new ChooseModulesDialog(parent, candidates, LocalizeValue.localizeTODO("Choose Module"));
                List<Module> chosen = chooser.showAndGetResult();

                if (chosen != null && !chosen.isEmpty()) {
                    for (Module module : chosen) {
                        myItems.add(new Item(module, ""));
                    }
                    sortItems();
                    fireTableDataChanged();

                    for (int i = 0; i < myItems.size(); i++) {
                        if (myItems.get(i).module.equals(chosen.get(0))) {
                            return i;
                        }
                    }
                }
            }

            return -1;
        }

        private void sortItems() {
            myItems.sort(Comparator.comparing(o -> o.module.getName()));
        }

        @Override
        public int getRowCount() {
            return myItems.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Item item = myItems.get(rowIndex);
            return columnIndex == 0 ? item.module : item.option;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Item item = myItems.get(rowIndex);
            item.option = ((String) aValue).trim();
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public void removeRow(int idx) {
            myItems.remove(idx);
            fireTableRowsDeleted(idx, idx);
        }
    }

    // upstream: com.intellij.compiler.options.ModuleTableCellRenderer (IntelliJ java-compiler-impl)
    private static class ModuleTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            try {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            finally {
                Module module = (Module) value;
                if (module != null) {
                    setText(module.getName());
                    // upstream: ModuleType.get(module).getIcon()
                    setIcon(TargetAWT.to(AllIcons.Nodes.Module));
                }
                else {
                    setText("");
                    setIcon(null);
                }
            }
        }
    }
}
