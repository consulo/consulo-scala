package org.jetbrains.plugins.scala.lang.refactoring.introduceField;

import consulo.util.lang.StringUtil;


import com.intellij.java.impl.refactoring.HelpID;
import consulo.application.ApplicationManager;
import consulo.application.HelpManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition;
import org.jetbrains.plugins.scala.lang.psi.types.ScType;
import org.jetbrains.plugins.scala.lang.psi.types.TypePresentationContext;
import org.jetbrains.plugins.scala.lang.psi.types.TypePresentationContext$;
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator$;
import org.jetbrains.plugins.scala.lang.refactoring.ui.TypeAnnotationsSettingsLink;
import org.jetbrains.plugins.scala.lang.refactoring.util.NamedDialog;
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil;
import org.jetbrains.plugins.scala.lang.refactoring.util.ValidationReporter;
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings;
import org.jetbrains.plugins.scala.settings.annotations.*;
import scala.Some$;
import scala.collection.immutable.ArraySeq;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ScalaIntroduceFieldDialog extends DialogWrapper implements NamedDialog {
    private static final String PRIVATE = "Private";
    private static final String PROTECTED = "Protected";
    private static final String PUBLIC = "Public";

    private CheckBox myReplaceAllChb;
    private ComboBox<String> myTypeComboBox;
    private TextBoxWithHistory myNameTextBox;
    private RadioGroup<Boolean> myInitGroup;
    private RadioButton myInitInDeclarationRB;
    private RadioButton myInitInLocalScopeRB;

    private Label myTypeLabel;
    private VerticalLayout contentPane;
    private CheckBox mySpecifyTypeChb;
    private Hyperlink myLink;
    private CheckBox myVariableCheckBox;
    private ComboBox<String> visibilityComboBox;
    private TextBox visibilityTextField;

    private Project project;
    private ArraySeq<ScType> myTypes;
    private int occurrencesCount;
    private ValidationReporter reporter;
    private IntroduceFieldSettings mySettings;
    private ScTemplateDefinition myClass;

    private LinkedHashMap<String, ScType> myTypeMap = null;

    private static final String REFACTORING_NAME = ScalaBundle.message("introduce.field.title");

    @RequiredUIAccess
    public ScalaIntroduceFieldDialog(IntroduceFieldContext<ScExpression> ifc, IntroduceFieldSettings<ScExpression> settings) {
        super(ifc.project(), true);
        this.project = ifc.project();
        this.myTypes = ifc.types();
        this.occurrencesCount = ifc.occurrences().size();
        this.reporter = ifc.reporter();
        this.mySettings = settings;
        this.myClass = ifc.aClass();

        ScExpression expression = ScalaRefactoringUtil.expressionToIntroduce(ifc.element());

        createUI();

        setModal(true);
        setTitle(REFACTORING_NAME);
        init();
        setUpDialog();
        setUpNameComboBox(ifc.possibleNames());
        setUpTypeComboBox(expression);
        setUpHyperLink(expression);
        bindToSettings();
        updateOkStatus();
    }

    @RequiredUIAccess
    private void createUI() {
        mySpecifyTypeChb = CheckBox.create(LocalizeValue.of(ScalaBundle.message("specify.return.type.explicitly")));
        myLink = TypeAnnotationsSettingsLink.create(project);

        visibilityComboBox = ComboBox.create(PRIVATE, PROTECTED, PUBLIC);
        visibilityTextField = TextBox.create("");
        Label visibilityLabel = Label.create(LocalizeValue.localizeTODO("Visi&bility"));
        visibilityLabel.setTarget(visibilityComboBox);

        boolean nullText = myTypes.exists(ty -> ty.toString() == null);
        List<String> typeNames = new ArrayList<>();
        if (myTypes.size() != 0 && !nullText) {
            TypePresentationContext context = TypePresentationContext$.MODULE$.psiElementPresentationContext(myClass);
            myTypeMap = ScalaRefactoringUtil.getCompatibleTypeNames(myTypes, context);
            typeNames.addAll(myTypeMap.keySet());
        }
        myTypeComboBox = ComboBox.create(typeNames);
        if (!typeNames.isEmpty()) {
            myTypeComboBox.setValueByIndex(0);
        }
        myTypeLabel = Label.create(LocalizeValue.localizeTODO("T&ype"));
        myTypeLabel.setTarget(myTypeComboBox);

        myNameTextBox = TextBoxWithHistory.create();
        Label nameLabel = Label.create(LocalizeValue.localizeTODO("&Name"));
        nameLabel.setTarget(myNameTextBox);

        myInitGroup = RadioGroup.create();
        myInitInDeclarationRB = myInitGroup.newButton(LocalizeValue.localizeTODO("&Field declaration"), true);
        myInitInLocalScopeRB = myInitGroup.newButton(LocalizeValue.localizeTODO("&Local scope"), false);

        myVariableCheckBox = CheckBox.create(LocalizeValue.localizeTODO("&Variable"));
        myReplaceAllChb = CheckBox.create(LocalizeValue.localizeTODO("Replace &all occurrences"));

        contentPane = VerticalLayout.create();
        contentPane.add(HorizontalLayout.create().add(mySpecifyTypeChb).add(myLink));
        contentPane.add(FormBuilder.create()
                .addLabeled(visibilityLabel, HorizontalLayout.create().add(visibilityComboBox).add(visibilityTextField))
                .addLabeled(myTypeLabel, myTypeComboBox)
                .addLabeled(nameLabel, myNameTextBox)
                .build());
        contentPane.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.localizeTODO("Initialize in")))
                .add(myInitInDeclarationRB)
                .add(myInitInLocalScopeRB));
        contentPane.add(myVariableCheckBox);
        contentPane.add(myReplaceAllChb);
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return (JComponent) TargetAWT.to(contentPane);
    }

    @Override
    @Nullable
    public String getEnteredName() {
        String text = StringUtil.notNullize(myNameTextBox.getValue());
        return text != null && !text.isEmpty() ? text : null;
    }

    @Override
    public boolean isReplaceAllOccurrences() {
        return myReplaceAllChb.getValueOrError();
    }

    private ScType getSelectedType() {
        if (mySpecifyTypeChb.getValueOrError()) {
            return myTypeMap.get(myTypeComboBox.getValue());
        }

        return null;
    }

    private boolean isPublic() {
        return PUBLIC.equals(visibilityComboBox.getValue());
    }

    private String getVisibility() {
        if (isPublic()) return "";

        if (getVisibilityEncloser().equals("")) {
            return visibilityComboBox.getValue().toLowerCase() + " ";
        }

        return visibilityComboBox.getValue().toLowerCase() + "[" + getVisibilityEncloser() + "] ";
    }

    private String getVisibilityEncloser() {
        return StringUtil.notNullize(visibilityTextField.getValue());
    }

    @RequiredUIAccess
    private void bindToSettings() {
        myInitGroup.addValueListener(value -> {
            mySettings.setInitInDeclaration(myInitInDeclarationRB.getValueOrError());
            readSettings();
        });

        myReplaceAllChb.addValueListener(e -> {
            mySettings.setReplaceAll(myReplaceAllChb.getValueOrError());
            readSettings();
        });

        myVariableCheckBox.addValueListener(e -> {
            mySettings.setDefineVar(myVariableCheckBox.getValueOrError());
            readSettings();
        });

        visibilityComboBox.addValueListener(e -> {
            visibilityTextField.setEnabled(!isPublic());
            mySettings.setVisibilityLevel(getVisibility());
            readSettings();
        });
    }

    @RequiredUIAccess
    private void setUpDialog() {
        readSettings();

        // Type specification
        if (myTypeMap == null) {
            myTypeComboBox.setEnabled(false);
        }

        if (occurrencesCount > 1) {
            myReplaceAllChb.setLabelText(LocalizeValue.localizeTODO("Replace &all occurrences" + " (" + ScalaBundle.message("field.occurrences", occurrencesCount) + ")"));
        }
    }

    @RequiredUIAccess
    private void readSettings() {
        myVariableCheckBox.setEnabled(mySettings.defineVarChbEnabled());

        myInitInDeclarationRB.setEnabled(mySettings.initInDeclarationEnabled());
        myInitInLocalScopeRB.setEnabled(mySettings.initLocallyEnabled());
        myReplaceAllChb.setEnabled(mySettings.replaceAllChbEnabled());

        myVariableCheckBox.setValue(mySettings.defineVar(), false);
        myInitGroup.setValue(mySettings.initInDeclaration(), false);
        myReplaceAllChb.setValue(mySettings.replaceAll(), false);

        if (mySettings.visibilityLevel().contains("protected")) {
            visibilityComboBox.setValue(PROTECTED, false);
        } else if (mySettings.visibilityLevel().contains("private")) {
            visibilityComboBox.setValue(PRIVATE, false);
        } else {
            visibilityComboBox.setValue(PUBLIC, false);
        }
    }

    @RequiredUIAccess
    private void setUpNameComboBox(Set<String> possibleNames) {
        List<String> names = new ArrayList<>(possibleNames);
        myNameTextBox.setHistory(names);
        if (!names.isEmpty()) {
            myNameTextBox.setValue(names.get(0), false);
        }
        myNameTextBox.addValueListener(e -> updateOkStatus());
    }

    private boolean needsTypeAnnotation(ScExpression expression) {
        Declaration declaration = Declaration$.MODULE$.apply(
                Visibility$.MODULE$.apply(getVisibility()),
                false,
                false,
                false,
                false
        );
        return ScalaTypeAnnotationSettings$.MODULE$.apply(expression.getProject())
                .isTypeAnnotationRequiredFor(
                        declaration,
                        Location$.MODULE$.apply(myClass),
                        Some$.MODULE$.apply(new Expression(expression))
                );
    }

    @RequiredUIAccess
    private void setUpTypeComboBox(final ScExpression expression) {
        mySpecifyTypeChb.setValue(needsTypeAnnotation(expression), false);
        updateEnablingTypeList();

        mySpecifyTypeChb.addValueListener(e -> updateEnablingTypeList());

        visibilityComboBox.addValueListener(e -> {
            mySpecifyTypeChb.setValue(needsTypeAnnotation(expression), false);
            updateEnablingTypeList();
        });
    }

    @RequiredUIAccess
    private void setUpHyperLink(final ScExpression expression) {
        myLink.addHyperlinkListener(e -> ApplicationManager.getApplication().invokeLater(() -> {
            mySpecifyTypeChb.setValue(needsTypeAnnotation(expression), false);
            updateEnablingTypeList();
        }));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return (JComponent) TargetAWT.to(myNameTextBox);
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    private void saveSettings() {
        ScalaApplicationSettings scalaSettings = ScalaApplicationSettings.getInstance();
        if (myVariableCheckBox.isEnabled())
            scalaSettings.INTRODUCE_FIELD_IS_VAR = myVariableCheckBox.getValueOrError();
        if (myReplaceAllChb.isEnabled())
            scalaSettings.INTRODUCE_FIELD_REPLACE_ALL = myReplaceAllChb.getValueOrError();

        scalaSettings.INTRODUCE_FIELD_VISIBILITY = getVisibility();
        mySettings.setVisibilityLevel(getVisibility());

        if (myInitInDeclarationRB.isEnabled() && myInitInLocalScopeRB.isEnabled()) {
            scalaSettings.INTRODUCE_FIELD_INITIALIZE_IN_DECLARATION = myInitInDeclarationRB.getValueOrError();
        }
    }

    @Override
    protected void doOKAction() {
        if (!reporter.isOK(this)) return;
        saveSettings();

        mySettings.setName(getEnteredName());
        mySettings.setType(getSelectedType());
        super.doOKAction();
    }


    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HelpID.INTRODUCE_FIELD);
    }

    private void updateOkStatus() {
        String text = getEnteredName();
        setOKActionEnabled(ScalaNamesValidator$.MODULE$.isIdentifier(text));
    }

    @RequiredUIAccess
    private void updateEnablingTypeList() {
        myTypeComboBox.setEnabled(mySpecifyTypeChb.getValueOrError());
        myTypeLabel.setEnabled(mySpecifyTypeChb.getValueOrError());
    }
}
