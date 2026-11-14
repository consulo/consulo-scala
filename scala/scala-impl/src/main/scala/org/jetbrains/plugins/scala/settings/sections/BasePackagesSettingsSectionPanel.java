package org.jetbrains.plugins.scala.settings.sections;

import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ui.awt.ChooseModulesDialog;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ItemRemovable;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.*;

import static org.jetbrains.plugins.scala.settings.ScalaProjectSettings.getInstance;

public class BasePackagesSettingsSectionPanel extends SettingsSectionPanel {
    private final RadioGroup<Boolean> myInheritBasePackages;
    private final JPanel myBasePackagesPanel;
    private final VerticalLayout rootPanel;
    private final JBTable myBasePackagesTable;

    @RequiredUIAccess
    public BasePackagesSettingsSectionPanel(Project project) {
        super(project);

        myInheritBasePackages = RadioGroup.create();
        RadioButton inheritBasePackagesRadioButton = myInheritBasePackages.newButton(message("scala.project.settings.form.base.package.inherit"), true);
        RadioButton useCustomBasePackagesRadioButton = myInheritBasePackages.newButton(message("scala.project.settings.form.base.package.custom"), false);

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
        // upstream table editor has no unified UI counterpart, stays Swing
        myBasePackagesPanel = new JPanel(new BorderLayout());
        myBasePackagesPanel.add(tableComp);

        myInheritBasePackages.addValueListener(inherit -> myBasePackagesTable.setEnabled(!Boolean.TRUE.equals(inherit)));

        rootPanel = VerticalLayout.create();
        rootPanel.add(withHelp(inheritBasePackagesRadioButton, ScalaBundle.message("base.package.help")));
        rootPanel.add(useCustomBasePackagesRadioButton);
        rootPanel.add(TargetAWT.wrap(myBasePackagesPanel));

        reset();
    }

    @Override
    consulo.ui.Component getRootPanel() {
        return rootPanel;
    }

    @Override
    boolean isModified() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        return scalaProjectSettings.isInheritBasePackages() != isInheritBasePackages() ||
                !scalaProjectSettings.getCustomBasePackages().equals(getCustomBasePackages());
    }

    @Override
    void apply() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        scalaProjectSettings.setInheritBasePackages(isInheritBasePackages());
        scalaProjectSettings.setCustomBasePackages(getCustomBasePackages());
    }

    @Override
    void reset() {
        final ScalaProjectSettings scalaProjectSettings = getInstance(myProject);

        myInheritBasePackages.setValue(scalaProjectSettings.isInheritBasePackages());
        myBasePackagesTable.setEnabled(!scalaProjectSettings.isInheritBasePackages());
        setCustomBasePackages(scalaProjectSettings.getCustomBasePackages());
    }


    private boolean isInheritBasePackages() {
        return Boolean.TRUE.equals(myInheritBasePackages.getValue());
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
                    setIcon(TargetAWT.to(PlatformIconGroup.nodesModule()));
                }
                else {
                    setText("");
                    setIcon(null);
                }
            }
        }
    }
}
