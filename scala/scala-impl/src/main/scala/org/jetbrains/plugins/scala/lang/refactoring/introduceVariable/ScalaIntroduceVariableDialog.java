package org.jetbrains.plugins.scala.lang.refactoring.introduceVariable;

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

public class ScalaIntroduceVariableDialog extends DialogWrapper implements NamedDialog {
    private CheckBox declareVariableCheckBox;
    private CheckBox myCbReplaceAllOccurences;
    private ComboBox<String> myTypeComboBox;
    private TextBoxWithHistory myNameTextBox;
    private Label myTypeLabel;
    private CheckBox mySpecifyTypeChb;
    private VerticalLayout contentPane;

    private Project project;
    private ArraySeq<ScType> myTypes;
    private int occurrencesCount;
    private ValidationReporter reporter;
    private ScExpression expression;

    private LinkedHashMap<String, ScType> myTypeMap = null;

    private static final String REFACTORING_NAME = ScalaBundle.message("introduce.variable.title");


    @RequiredUIAccess
    public ScalaIntroduceVariableDialog(final Project project,
                                        ArraySeq<ScType> myTypes,
                                        int occurrencesCount,
                                        ValidationReporter reporter,
                                        String[] possibleNames,
                                        ScExpression expression) {
        super(project, true);
        this.project = project;
        this.myTypes = myTypes;
        this.occurrencesCount = occurrencesCount;
        this.reporter = reporter;
        this.expression = expression;

        boolean nullText = myTypes.exists((ty) -> ty.toString() == null);
        List<String> typeNames = new ArrayList<>();
        // Type specification
        if (myTypes.nonEmpty() && !nullText) {
            TypePresentationContext context = TypePresentationContext$.MODULE$.psiElementPresentationContext(expression);
            myTypeMap = ScalaRefactoringUtil.getCompatibleTypeNames(myTypes, context);
            typeNames.addAll(myTypeMap.keySet());
        }

        createUI(possibleNames, typeNames);

        setModal(true);
        setTitle(REFACTORING_NAME);
        init();
        setUpDialog();
        updateOkStatus();
    }

    @RequiredUIAccess
    private void createUI(String[] possibleNames, List<String> typeNames) {
        mySpecifyTypeChb = CheckBox.create(LocalizeValue.of(ScalaBundle.message("specify.return.type.explicitly")));
        Hyperlink link = TypeAnnotationsSettingsLink.create(project);
        link.addHyperlinkListener(e -> ApplicationManager.getApplication().invokeLater(() -> {
            mySpecifyTypeChb.setValue(needsTypeAnnotation());
            updateEnablingTypeList();
        }));

        myTypeComboBox = ComboBox.create(typeNames);
        if (!typeNames.isEmpty()) {
            myTypeComboBox.setValueByIndex(0);
        }
        myTypeLabel = Label.create(LocalizeValue.localizeTODO("T&ype"));
        myTypeLabel.setTarget(myTypeComboBox);

        myNameTextBox = TextBoxWithHistory.create(possibleNames.length > 0 ? possibleNames[0] : "").setHistory(possibleNames);
        myNameTextBox.addValueListener(e -> updateOkStatus());
        Label nameLabel = Label.create(LocalizeValue.localizeTODO("&Name"));
        nameLabel.setTarget(myNameTextBox);

        declareVariableCheckBox = CheckBox.create(LocalizeValue.localizeTODO("&Variable"));
        myCbReplaceAllOccurences = CheckBox.create(LocalizeValue.localizeTODO("Replace &all occurrences"));

        contentPane = VerticalLayout.create();
        contentPane.add(HorizontalLayout.create().add(mySpecifyTypeChb).add(link));
        contentPane.add(FormBuilder.create().addLabeled(myTypeLabel, myTypeComboBox).addLabeled(nameLabel, myNameTextBox).build());
        contentPane.add(declareVariableCheckBox);
        contentPane.add(myCbReplaceAllOccurences);
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
        return myCbReplaceAllOccurences.getValueOrError();
    }

    public boolean isDeclareVariable() {
        return declareVariableCheckBox.getValueOrError();
    }

    public ScType getSelectedType() {
        if (myTypeComboBox.isEnabled()) {
            return myTypeMap.get(myTypeComboBox.getValue());
        }

        return null;
    }

    @RequiredUIAccess
    private void setUpDialog() {
        setUpSpecifyTypeChb();

        // Replace occurences
        if (occurrencesCount > 1) {
            myCbReplaceAllOccurences.setValue(false);
            myCbReplaceAllOccurences.setEnabled(true);
            myCbReplaceAllOccurences.setLabelText(LocalizeValue.localizeTODO("Replace &all occurrences" + ScalaBundle.message("multi.occurrences", occurrencesCount)));
        } else {
            myCbReplaceAllOccurences.setValue(false);
            myCbReplaceAllOccurences.setEnabled(false);
        }

        updateEnablingTypeList();
    }

    // TODO Are all non-local variables now "private"?
    // TODO Is there a scope selection?
    private boolean needsTypeAnnotation() {
        Declaration declaration = Declaration$.MODULE$.apply(
                Visibility$.MODULE$.apply("private"),
                false,
                false,
                false,
                false
        );
        return ScalaTypeAnnotationSettings$.MODULE$.apply(expression.getProject())
                .isTypeAnnotationRequiredFor(
                        declaration,
                        Location$.MODULE$.apply(expression),
                        Some$.MODULE$.apply(new Expression(expression))
                );
    }

    @RequiredUIAccess
    private void setUpSpecifyTypeChb() {
        mySpecifyTypeChb.setValue(needsTypeAnnotation());

        mySpecifyTypeChb.addValueListener(e -> updateEnablingTypeList());
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return (JComponent) TargetAWT.to(myNameTextBox);
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    protected void doOKAction() {
        if (!reporter.isOK(this)) {
            return;
        }
        if (mySpecifyTypeChb.isEnabled()) {
            ScalaApplicationSettings.getInstance().INTRODUCE_VARIABLE_EXPLICIT_TYPE = mySpecifyTypeChb.getValueOrError();
        }
        super.doOKAction();
    }


    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HelpID.INTRODUCE_VARIABLE);
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
