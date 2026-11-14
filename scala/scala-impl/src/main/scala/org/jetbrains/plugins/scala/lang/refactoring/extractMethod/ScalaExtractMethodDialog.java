package org.jetbrains.plugins.scala.lang.refactoring.extractMethod;

import com.intellij.java.analysis.impl.refactoring.util.VariableData;
import com.intellij.java.impl.refactoring.util.ParameterTablePanel;
import consulo.application.ApplicationManager;
import consulo.language.editor.refactoring.ui.MethodSignatureComponent;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.api.ScalaFileType;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateBody;
import org.jetbrains.plugins.scala.lang.psi.dataFlow.impl.reachingDefs.VariableInfo;
import org.jetbrains.plugins.scala.lang.psi.types.ScType;
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator$;
import org.jetbrains.plugins.scala.lang.refactoring.ui.TypeAnnotationsSettingsLink;
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings;
import scala.Option;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ScalaExtractMethodDialog extends DialogWrapper {
  private static final String PRIVATE = "Private";
  private static final String PROTECTED = "Protected";
  private static final String PUBLIC = "Public";
  private static final String TUPLE = "Tuple";
  private static final String INNER_CASE_CLASS = "Inner case class";
  private static final String INNER_CLASS = "Inner class";

  private final String REFACTORING_NAME = ScalaBundle.message("extract.method.title");
  private VerticalLayout contentPane;
  private TextBox editorTextField;
  private CheckBox mySpecifyTypeChb;
  private Hyperlink myLink;
  private ComboBox<String> multipleOutputCombobox;
  private ComboBox<String> visibilityComboBox;
  private TextBox visibiltyTextField;
  private TextBox multipleOutputTextField;
  private Label visibilityLabel;
  private Label multipleOutputLabel;

  private boolean isDefaultClassName = true;
  private final MethodSignatureComponent mySignaturePreview;

  private ScalaExtractMethodSettings settings = null;
  private final Project myProject;
  private final PsiElement[] myElements;
  private final Option<ScType> myHasReturn;

  private final PsiElement mySibling;

  private final VariableInfo[] myInput;
  private final VariableInfo[] myOutput;
  private ScalaExtractMethodDialog.ScalaParameterTablePanel parameterTablePanel;
  private final boolean myLastReturn;
  private final Option<ScType> myLastMeaningful;

  private final boolean isInitialized;

  @RequiredUIAccess
  public ScalaExtractMethodDialog(final Project project, PsiElement[] elements, Option<ScType> hasReturn, boolean lastReturn,
                                  PsiElement sibling, VariableInfo[] input, VariableInfo[] output,
                                  Option<ScType> lastMeaningful) {
    super(project, true);

    myElements = elements;
    myProject = project;
    myHasReturn = hasReturn;
    myLastReturn = lastReturn;
    myInput = input;
    myOutput = output;
    myLastMeaningful = lastMeaningful;
    mySibling = sibling;
    mySignaturePreview = new MethodSignatureComponent("", project, ScalaFileType.INSTANCE);
    mySignaturePreview.setMinimumSize(new Dimension(500, 70));

    createUI();

    setModal(true);
    setTitle(REFACTORING_NAME);
    init();
    setUpDialog();

    isInitialized = true;

    updateSettings();
    setUpTypeChb();
    setUpHyperLink();
    updateSignature();

    updateOkStatus();
  }

  @RequiredUIAccess
  private void createUI() {
    multipleOutputCombobox = ComboBox.create(TUPLE, INNER_CASE_CLASS, INNER_CLASS);
    multipleOutputCombobox.setValue(TUPLE, false);
    multipleOutputTextField = TextBox.create();
    multipleOutputLabel = Label.create(LocalizeValue.localizeTODO("&Multiple output"));
    multipleOutputLabel.setTarget(multipleOutputCombobox);

    visibilityComboBox = ComboBox.create(PRIVATE, PROTECTED, PUBLIC);
    visibilityComboBox.setValue(PRIVATE, false);
    visibiltyTextField = TextBox.create();
    visibilityLabel = Label.create(LocalizeValue.localizeTODO("&Visibility"));
    visibilityLabel.setTarget(visibilityComboBox);

    editorTextField = TextBox.create("");
    Label nameLabel = Label.create(LocalizeValue.localizeTODO("&Name"));
    nameLabel.setTarget(editorTextField);

    TableLayout methodNamePanel = TableLayout.create(StaticPosition.TOP);
    methodNamePanel.add(multipleOutputLabel, TableLayout.cell(0, 0));
    methodNamePanel.add(multipleOutputCombobox, TableLayout.cell(0, 1));
    methodNamePanel.add(multipleOutputTextField, TableLayout.cell(0, 2).fill());
    methodNamePanel.add(visibilityLabel, TableLayout.cell(1, 0));
    methodNamePanel.add(visibilityComboBox, TableLayout.cell(1, 1));
    methodNamePanel.add(visibiltyTextField, TableLayout.cell(1, 2).fill());
    methodNamePanel.add(nameLabel, TableLayout.cell(2, 0));
    methodNamePanel.add(editorTextField, TableLayout.cell(2, 1).fill());

    ArrayList<VariableData> data = new ArrayList<VariableData>();
    for (VariableInfo aMyInput : myInput) {
      VariableData d = ScalaExtractMethodUtils.convertVariableData(aMyInput, myElements);
      if (d != null) data.add(d);
    }
    parameterTablePanel = new ScalaParameterTablePanel(myProject, data.toArray(new VariableData[0]));

    mySpecifyTypeChb = CheckBox.create(LocalizeValue.localizeTODO("Specify result &type"));
    myLink = TypeAnnotationsSettingsLink.create(myProject);

    contentPane = VerticalLayout.create();
    contentPane.add(methodNamePanel);
    contentPane.add(LabeledLayout.create(LocalizeValue.localizeTODO("Parameters"), TargetAWT.wrap(parameterTablePanel)));
    contentPane.add(HorizontalLayout.create().add(mySpecifyTypeChb).add(myLink));
    contentPane.add(LabeledLayout.create(LocalizeValue.localizeTODO("Signature preview"), TargetAWT.wrap(mySignaturePreview)));
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return (JComponent) TargetAWT.to(editorTextField);
  }

  @Override
  protected JComponent createCenterPanel() {
    return (JComponent) TargetAWT.to(contentPane);
  }

  private void updateOkStatus() {
    setOKActionEnabled(ScalaNamesValidator$.MODULE$.isIdentifier(getMethodName()) &&
        (isPublic() || getVisibilityEncloser().equals("") || ScalaNamesValidator$.MODULE$.isIdentifier(getVisibilityEncloser())) &&
        (isTuple() || ScalaNamesValidator$.MODULE$.isIdentifier(getMultipleOutputEncloser())));
  }

  private String getVisibilityEncloser() {
    return StringUtil.notNullize(visibiltyTextField.getValue());
  }

  private String getMultipleOutputEncloser() {
    return StringUtil.notNullize(multipleOutputTextField.getValue());
  }

  @RequiredUIAccess
  private void setUpDialog() {
    editorTextField.addValueListener(event -> {
      if (isDefaultClassName) updateClassName(StringUtil.capitalize(StringUtil.notNullize(editorTextField.getValue())));
      updateSignature();
    });

    setupVisibilityPanel();
    setupMultipleOutputsPanel();
    updateSignature();
  }

  @RequiredUIAccess
  private void updateClassName(String newName) {
    if (isDefaultClassName) {
      multipleOutputTextField.setValue(newName);
      isDefaultClassName = true;
    }
  }

  @RequiredUIAccess
  private void setupVisibilityPanel() {
    visibilityComboBox.addValueListener(e -> {
      visibiltyTextField.setEnabled(!isPublic());
      updateSignature();
    });

    visibiltyTextField.addValueListener(e -> updateSignature());

    setVisibilityPanelVisisbility(isVisibilitySectionAvailable());
  }

  @RequiredUIAccess
  private void setVisibilityPanelVisisbility(boolean value) {
    visibilityLabel.setVisible(value);
    visibilityComboBox.setVisible(value);
    visibiltyTextField.setVisible(value);
  }

  private boolean isVisibilitySectionAvailable() {
    return mySibling.getParent() instanceof ScTemplateBody;
  }

  private String getVisibility() {
    if (!isVisibilitySectionAvailable()) return "";
    if (isPublic()) return "";

    if (getVisibilityEncloser().equals("")) {
      return visibilityComboBox.getValue().toLowerCase() + " ";
    }

    return visibilityComboBox.getValue().toLowerCase() + "[" + getVisibilityEncloser() + "] ";
  }

  @RequiredUIAccess
  private void setMultipleOutputPanelVisibility(boolean value) {
    multipleOutputLabel.setVisible(value);
    multipleOutputCombobox.setVisible(value);
    multipleOutputTextField.setVisible(value);
  }

  @RequiredUIAccess
  protected void setupMultipleOutputsPanel() {
    updateSettings();
    setMultipleOutputPanelVisibility(settings.outputs().length > 1);

    multipleOutputCombobox.addValueListener(e -> {
      multipleOutputTextField.setEnabled(!isTuple());
      updateSignature();
    });

    multipleOutputTextField.addValueListener(e -> {
      if (!TUPLE.equals(multipleOutputCombobox.getValue())) {
        isDefaultClassName = false;
      }

      updateSignature();
    });

    multipleOutputTextField.setEnabled(!isTuple());
  }

  protected void updateSignature() {
    if (!isInitialized) return;

    updateOkStatus();
    updateSettings();
    String text = ScalaExtractMethodUtils.previewSignatureText(settings);
    mySignaturePreview.setSignature(text);
  }

  @Override
  protected void doOKAction() {
    updateSettings();
    super.doOKAction();
  }

  private boolean isInnerClass() {
    return INNER_CLASS.equals(multipleOutputCombobox.getValue());
  }

  private boolean isCaseClass() {
    return INNER_CASE_CLASS.equals(multipleOutputCombobox.getValue());
  }

  private boolean isPublic() {
    return PUBLIC.equals(visibilityComboBox.getValue());
  }

  private boolean isTuple() {
    return TUPLE.equals(multipleOutputCombobox.getValue());
  }

  private void updateSettings() {
    boolean createClass = isInnerClass() || isCaseClass();
    InnerClassSettings ics = new InnerClassSettings(createClass, getClassName(), getReturns(), isCaseClass());
    ScalaApplicationSettings.ReturnTypeLevel addReturnType = mySpecifyTypeChb.getValueOrError() ?
        ScalaApplicationSettings.ReturnTypeLevel.ADD : ScalaApplicationSettings.ReturnTypeLevel.REMOVE;

    settings = new ScalaExtractMethodSettings(getMethodName(), getParameters(), getReturns(),
        getVisibility(), mySibling, myElements, myHasReturn, addReturnType, myLastReturn, myLastMeaningful, ics);
  }

  @RequiredUIAccess
  private void setUpTypeChb() {
    mySpecifyTypeChb.setValue(ScalaExtractMethodUtils.isTypeAnnotationRequiredFor(settings, getVisibility()), false);

    mySpecifyTypeChb.addValueListener(e -> updateSignature());

    visibilityComboBox.addValueListener(e -> mySpecifyTypeChb.setValue(ScalaExtractMethodUtils.isTypeAnnotationRequiredFor(settings, getVisibility()), false));
  }

  @RequiredUIAccess
  private void setUpHyperLink() {
    myLink.addHyperlinkListener(e -> ApplicationManager.getApplication().invokeLater(() -> {
      mySpecifyTypeChb.setValue(ScalaExtractMethodUtils.isTypeAnnotationRequiredFor(settings, getVisibility()), false);
      updateSignature();
    }));
  }

  private String getClassName() {
    if (isCaseClass() || isInnerClass()) {
      return getMultipleOutputEncloser();
    }

    return "";
  }

  public ExtractMethodParameter[] getParameters() {
    VariableData[] data = parameterTablePanel.getVariableData();
    ArrayList<ExtractMethodParameter> list = new ArrayList<>();
    for (VariableData d : data) {
      ScalaVariableData variableData = (ScalaVariableData) d;
      ExtractMethodParameter param = ExtractMethodParameter.from(variableData);
      list.add(param);
    }
    return list.toArray(new ExtractMethodParameter[0]);
  }

  public ExtractMethodOutput[] getReturns() {
    ArrayList<ExtractMethodOutput> list = new ArrayList<>();
    for (VariableInfo info : myOutput) {
      ScalaVariableData data = ScalaExtractMethodUtils.convertVariableData(info, myElements);
      list.add(ExtractMethodOutput.from(data));
    }
    return list.toArray(new ExtractMethodOutput[0]);
  }

  private String getMethodName() {
    return StringUtil.notNullize(editorTextField.getValue());
  }

  public ScalaExtractMethodSettings getSettings() {
    return settings;
  }

  private class ScalaParameterTablePanel extends ParameterTablePanel {
    public ScalaParameterTablePanel(Project project, VariableData[] variableData) {
      super(project, variableData);
    }

    @Override
    protected void updateSignature() {
      ScalaExtractMethodDialog.this.updateSignature();
    }

    @Override
    protected void doEnterAction() {
    }

    @Override
    protected void doCancelAction() {
    }
  }
}