package org.jetbrains.plugins.scala.lang.refactoring.introduceVariable;

import consulo.util.lang.StringUtil;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement;
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement$;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTrait;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition;
import org.jetbrains.plugins.scala.lang.refactoring.util.*;
import scala.Tuple2;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator$;

public class ScalaIntroduceTypeAliasDialog extends DialogWrapper implements NamedDialog {
    private VerticalLayout contentPane;
    private TextBoxWithHistory myNameTextBox;
    private ComboBox<ScopeItem> myScopeCombobox;
    private CheckBox myCbReplaceAllOccurences;
    private TextBox myTypeTextField;
    private CheckBox myReplaceCompanionObjectOcc;
    private CheckBox myReplaceInInheritors;

    private Project project;
    private ScTypeElement myTypeElement;
    private ConflictsReporter conflictsReporter;
    private ScopeItem currentScope;
    private Map<ScopeItem, Tuple2<ScTypeElement[], ScalaTypeValidator[]>> inheritanceDataMap;

    private static final String REFACTORING_NAME = ScalaBundle.message("introduce.type.alias.title");

    @RequiredUIAccess
    public ScalaIntroduceTypeAliasDialog(Project project,
                                         ScTypeElement myTypeElement,
                                         ScopeItem[] possibleScopes,
                                         ScopeItem mainScope,
                                         ConflictsReporter conflictReporter) {
        super(project, true);
        this.project = project;
        this.myTypeElement = myTypeElement;
        this.currentScope = mainScope;
        this.conflictsReporter = conflictReporter;
        this.inheritanceDataMap = new HashMap<ScopeItem, Tuple2<ScTypeElement[], ScalaTypeValidator[]>>();

        createUI(possibleScopes, mainScope);

        setModal(true);
        setTitle(REFACTORING_NAME);
        init();
        setUpDialog(currentScope);
        updateOkStatus();
    }

    @RequiredUIAccess
    private void createUI(ScopeItem[] possibleScopes, ScopeItem mainScope) {
        myTypeTextField = TextBox.create(ScTypeElement$.MODULE$.calcType(myTypeElement).toString());
        myTypeTextField.setEnabled(false);
        Label typeLabel = Label.create(LocalizeValue.localizeTODO("Type:"));
        typeLabel.setTarget(myTypeTextField);

        myNameTextBox = TextBoxWithHistory.create();
        myNameTextBox.addValueListener(e -> updateOkStatus());
        Label nameLabel = Label.create(LocalizeValue.localizeTODO("&Name:"));
        nameLabel.setTarget(myNameTextBox);

        myScopeCombobox = ComboBox.create(Arrays.asList(possibleScopes));
        myScopeCombobox.setTextRenderer(item -> item == null ? LocalizeValue.empty() : LocalizeValue.of(item.toString()));
        if (mainScope != null) {
            myScopeCombobox.setValue(mainScope, false);
        } else if (possibleScopes.length > 0) {
            myScopeCombobox.setValue(possibleScopes[0], false);
            currentScope = possibleScopes[0];
        }
        updateNameComboBox(currentScope.availableNames());
        myScopeCombobox.addValueListener(e -> {
            ScopeItem item = e.getValue();
            if (item != null) {
                currentScope = item;
                setUpOccurrences(item);
                updateNameComboBox(item.availableNames());
            }
        });

        myCbReplaceAllOccurences = CheckBox.create(LocalizeValue.localizeTODO("Replace &all occurrences"));
        myReplaceInInheritors = CheckBox.create(LocalizeValue.localizeTODO("Replace occurences in class inheritors"));
        myReplaceCompanionObjectOcc = CheckBox.create(LocalizeValue.localizeTODO("Replace occurrences available from companion class"));

        contentPane = VerticalLayout.create();
        contentPane.add(FormBuilder.create()
                .addLabeled(typeLabel, myTypeTextField)
                .addLabeled(nameLabel, myNameTextBox)
                .addLabeled(LocalizeValue.localizeTODO("Scope "), myScopeCombobox)
                .build());
        contentPane.add(myCbReplaceAllOccurences);
        contentPane.add(myReplaceInInheritors);
        contentPane.add(myReplaceCompanionObjectOcc);
    }

    @Override
    protected void doOKAction() {
        if (currentScope instanceof SimpleScopeItem) {
            if (!handleInheritedClasses((SimpleScopeItem) currentScope)) {
                return;
            }
        }
        ScopeItem newScope = currentScope;

        ScalaValidator validator = null;
        if (currentScope instanceof PackageScopeItem packageScopeItem) {
            currentScope = ScopeSuggester.handleOnePackage(myTypeElement, packageScopeItem.name(),
                    (PsiDirectory) packageScopeItem.fileEncloser(), conflictsReporter, myTypeElement.getProject(),
                    isReplaceAllOccurrences(), getEnteredName());

            validator = ((PackageScopeItem) currentScope).validator();
        } else if (currentScope instanceof SimpleScopeItem) {
            validator = ((SimpleScopeItem) currentScope).typeValidator();
        }

        if (validator != null) {
            if (isConflictReported(validator)) {
                currentScope = newScope;
                return;
            }
        }

        super.doOKAction();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return (JComponent) TargetAWT.to(myNameTextBox);
    }

    @RequiredUIAccess
    private void updateNameComboBox(Set<String> possibleNames) {
        List<String> names = new ArrayList<>(possibleNames);
        myNameTextBox.setHistory(names);
        myNameTextBox.setValue(names.isEmpty() ? "" : names.get(0));
    }

    @RequiredUIAccess
    private void setUpOccurrences(ScopeItem item) {
        setUpCompanionObjOcc(item);
        setUpReplaceInInheritors(item);

        myCbReplaceAllOccurences.setValue(false);
        if (item instanceof PackageScopeItem) {
            myCbReplaceAllOccurences.setEnabled(true);
            myCbReplaceAllOccurences.setLabelText(LocalizeValue.of(ScalaBundle.message("replace.all.occurrences")));
        } else if (item instanceof SimpleScopeItem simpleScopeItem) {
            int occurrencesCount = simpleScopeItem.usualOccurrences().length;
            if (occurrencesCount > 1) {
                myCbReplaceAllOccurences.setEnabled(true);
                myCbReplaceAllOccurences.setValue(true);
                myCbReplaceAllOccurences.setLabelText(LocalizeValue.of(ScalaBundle.message("replace.all.x.occurrences", occurrencesCount)));
            } else {
                myCbReplaceAllOccurences.setEnabled(false);
            }
        }
    }

    @RequiredUIAccess
    private void setUpCompanionObjOcc(ScopeItem item) {
        myReplaceCompanionObjectOcc.setValue(false);
        if (item instanceof SimpleScopeItem simpleScopeItem) {
            int companionObjOccCount = simpleScopeItem.occurrencesInCompanion().length;
            if (companionObjOccCount > 0) {
                myReplaceCompanionObjectOcc.setEnabled(true);
                String compObjectOccurrences = companionObjOccCount == 1 ? ScalaBundle.message("1.occurrence") : ScalaBundle.message("multi.occurrences", companionObjOccCount);
                myReplaceCompanionObjectOcc.setLabelText(LocalizeValue.of(ScalaBundle.message("replace.occurrences.available.from.companion.class", compObjectOccurrences)));
            } else {
                myReplaceCompanionObjectOcc.setEnabled(false);
            }
        }
    }

    private void updateOkStatus() {
        String text = getEnteredName();
        setOKActionEnabled(ScalaNamesValidator$.MODULE$.isIdentifier(text));
    }

    @RequiredUIAccess
    private void setUpDialog(ScopeItem scopeItem) {
        setUpOccurrences(scopeItem);
    }

    @RequiredUIAccess
    private void setUpReplaceInInheritors(ScopeItem scope) {
        myReplaceInInheritors.setValue(false);
        myReplaceInInheritors.setEnabled(false);
        if (scope instanceof SimpleScopeItem simpleScopeItem) {
            if (simpleScopeItem.isClass() || simpleScopeItem.isTrait()) {
                myReplaceInInheritors.setEnabled(true);
            } else {
                myReplaceInInheritors.setEnabled(false);
            }
        }
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return (JComponent) TargetAWT.to(contentPane);
    }

    @Override
    public boolean isReplaceAllOccurrences() {
        return myCbReplaceAllOccurences.getValueOrError();
    }

    public boolean isReplaceOccurrenceIncompanionObject() {
        return myReplaceCompanionObjectOcc.getValueOrError();
    }

    public boolean isReplaceOccurrenceInInheritors() {
        return myReplaceInInheritors.getValueOrError();
    }


    @Override
    @Nullable
    public String getEnteredName() {
        String text = StringUtil.notNullize(myNameTextBox.getValue());
        return text != null && !text.isEmpty() ? text : null;
    }

    public ScopeItem getSelectedScope() {
        return currentScope;
    }

    //return false if there is conflict in validator
    private boolean handleInheritedClasses(SimpleScopeItem simpleScopeItem) {
        ScTypeDefinition classOrTrait = PsiTreeUtil.getParentOfType(simpleScopeItem.fileEncloser(), ScClass.class, ScTrait.class);

        if (myReplaceInInheritors.getValueOrError() && (classOrTrait != null)) {
            ScopeItem selectedScope = getSelectedScope();

            if (selectedScope == null) {
                return true;
            }

            Tuple2<ScTypeElement[], ScalaTypeValidator[]> inheritors;
            if (inheritanceDataMap.containsKey(selectedScope)) {
                inheritors = inheritanceDataMap.get(selectedScope);
            } else {
                inheritors = ScalaRefactoringUtil.getOccurrencesInInheritors(myTypeElement, classOrTrait);
                inheritanceDataMap.put(selectedScope, inheritors);
            }

            simpleScopeItem.setInheritedOccurrences(inheritors._1());

            for (ScalaTypeValidator validator : inheritors._2()) {
                if (isConflictReported(validator)) {
                    return false;
                }
            }

            return true;
        }
        return true;
    }

    private boolean isConflictReported(ScalaValidator validator) {
        ValidationReporter reporter = new ValidationReporter(project, conflictsReporter, validator);
        return !reporter.isOK(this);
    }
}
