package org.jetbrains.plugins.scala.lang.refactoring.extractTrait;

import consulo.util.lang.StringUtil;

import com.intellij.java.impl.refactoring.ui.PackageNameReferenceEditorCombo;
import consulo.application.ApplicationManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator$;

public class ScalaExtractTraitDialog extends DialogWrapper {

  private static final String REFACTORING_NAME = ScalaBundle.message("extract.trait.name");
  private static final String DESTINATION_PACKAGE_RECENT_KEY = "ExtractTrait.RECENT_KEYS";

  private final ScTemplateDefinition mySourceClass;
  private final Project myProject;
  private final List<ScalaExtractMemberInfo> myMembers;
  private VerticalLayout contentPane;
  private TextBox traitNameFld;
  private PackageNameReferenceEditorCombo myPackageNameField;

  @RequiredUIAccess
  public ScalaExtractTraitDialog(Project project, ScTemplateDefinition sourceClass) {
    super(project, false);
    mySourceClass = sourceClass;
    myProject = mySourceClass.getProject();
    myMembers = ExtractSuperUtil.possibleMembersToExtractAsJava(sourceClass);
    setTitle(ScalaBundle.message("extract.trait.title"));
    setupDialog();
    init();
    updateOkStatus();
  }

  public List<ScalaExtractMemberInfo> getSelectedMembers() {
    ArrayList<ScalaExtractMemberInfo> result = new ArrayList<ScalaExtractMemberInfo>();
    for (ScalaExtractMemberInfo info : myMembers) {
      if (info.isChecked()) {
        result.add(info);
      }
    }
    return result;
  }

  public String getTraitName() {
    return StringUtil.notNullize(traitNameFld.getValue());
  }

  public String getPackageName() {
    return myPackageNameField.getText();
  }

  @RequiredUIAccess
  private void setupDialog() {
    setOKButtonText("Refactor");
    setOKButtonMnemonic('R');

    TextBox sourceClassFld = TextBox.create(ExtractSuperUtil.classPresentableName(mySourceClass));
    sourceClassFld.setEditable(false);
    traitNameFld = TextBox.create();
    myPackageNameField = createPackageNameField();

    contentPane = VerticalLayout.create();
    contentPane.add(Label.create(LocalizeValue.of(ScalaBundle.message("extract.trait.top.label.text"))));
    contentPane.add(sourceClassFld);
    contentPane.add(Label.create(LocalizeValue.of(ScalaBundle.message("extract.trait.name"))));
    contentPane.add(traitNameFld);
    contentPane.add(Label.create(LocalizeValue.of(ScalaBundle.message("extract.trait.package.label"))));
    contentPane.add(TargetAWT.wrap(myPackageNameField));
    contentPane.add(TargetAWT.wrap(createMemberSelectionPanel()));

    traitNameFld.addValueListener(e -> updateOkStatus());
  }

  private JPanel createMemberSelectionPanel() {
    String title = ScalaBundle.message("members.to.extract");
    String abstractHeader = ScalaBundle.message("extract.abstracts");
    return new ScalaExtractMembersSelectionPanel(title, myMembers, abstractHeader);
  }

  protected PackageNameReferenceEditorCombo createPackageNameField() {
    String name = ExtractSuperUtil.packageName(mySourceClass);
    return new PackageNameReferenceEditorCombo(name, myProject, DESTINATION_PACKAGE_RECENT_KEY,
        RefactoringBundle.message("choose.destination.package"));
  }

  private void updateOkStatus() {
    setOKActionEnabled(ScalaNamesValidator$.MODULE$.isIdentifier(getTraitName()));
  }

  @Override
  protected void doOKAction() {
    String pckgWarning = ExtractSuperUtil.checkPackage(getPackageName(), getTraitName(), mySourceClass);
    if (pckgWarning != null) {
      showErrorMessage(pckgWarning);
      return;
    }

    super.doOKAction();
  }

  private void showErrorMessage(String message) {
    if (ApplicationManager.getApplication().isUnitTestMode()) throw new RuntimeException(message);
    CommonRefactoringUtil.showErrorMessage(REFACTORING_NAME, message, null, myProject);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return (JComponent) TargetAWT.to(contentPane);
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return (JComponent) TargetAWT.to(traitNameFld);
  }
}
