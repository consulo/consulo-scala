package org.jetbrains.plugins.scala.lang.scaladoc.generate;

import consulo.util.lang.StringUtil;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.plugins.scala.ScalaBundle;

public class ScaladocConsoleRunConfigurationForm {
  private final Component myPanel1;
  private final TextBox additionalFlagsField;
  private final FileChooserTextBoxBuilder.Controller destDirChooser;
  private final CheckBox myOpenInBrowserCheckBox;
  private final CheckBox myVerboseCheckBox;
  private final TextBox docTitle;
  private final TextBox maxHeapSizeField;
  private final Project project;

  @RequiredUIAccess
  public ScaladocConsoleRunConfigurationForm(Project project) {
    this.project = project;

    String title = ScalaBundle.message("output.dir");
    destDirChooser = FileChooserTextBoxBuilder.create(project)
            .dialogTitle(title)
            .fileChooserDescriptor(new FileChooserDescriptor(false, true, false, false, false, false)
                    .withTitle(title)
                    .withFileFilter(VirtualFile::isDirectory))
            .build();
    additionalFlagsField = TextBox.create();
    maxHeapSizeField = TextBox.create();
    docTitle = TextBox.create();
    myOpenInBrowserCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Open in Browser"));
    myVerboseCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Verbose"));

    FormBuilder formBuilder = FormBuilder.create()
            .addLabeled(LocalizeValue.localizeTODO("Output Directory"), destDirChooser.getComponent())
            .addLabeled(LocalizeValue.localizeTODO("Additional Parameters"), additionalFlagsField)
            .addLabeled(LocalizeValue.localizeTODO("Max Heap Size (mb)"), maxHeapSizeField)
            .addLabeled(LocalizeValue.localizeTODO("Doc Title"), docTitle);
    formBuilder.addBottom(myOpenInBrowserCheckBox);
    formBuilder.addBottom(myVerboseCheckBox);
    myPanel1 = LabeledLayout.create(LocalizeValue.localizeTODO("Scaladoc Parameters"), formBuilder.build());

    ScaladocSettings settings = ScaladocSettings.getInstance(project);
    if (settings.docTitle != null) {
      docTitle.setValue(settings.docTitle);
    } else {
      docTitle.setValue(ScalaBundle.message("documentation.for.project", project.getName()));
    }

    if (settings.additionalFlags != null) {
      additionalFlagsField.setValue(settings.additionalFlags);
    }

    if (settings.maxHeapSize != null) {
      maxHeapSizeField.setValue(settings.maxHeapSize);
    }

    if (settings.outputDir != null) {
      destDirChooser.setValue(settings.outputDir);
    }

    if (settings.verbose != null) {
      myVerboseCheckBox.setValue(settings.verbose);
    }

    if (settings.openInBrowser != null) {
      myOpenInBrowserCheckBox.setValue(settings.openInBrowser);
    }
  }

  public void saveSettings() {
    ScaladocSettings settings = ScaladocSettings.getInstance(project);
    settings.outputDir = getOutputDir();
    settings.additionalFlags = getAdditionalFlags();
    settings.openInBrowser = isShowInBrowser();
    settings.verbose = isVerbose();
    settings.docTitle = getDocTitle();
    settings.maxHeapSize = getMaxHeapSize();
  }

  public String getOutputDir() {
    return destDirChooser.getValue();
  }

  public String getAdditionalFlags() {
    return StringUtil.notNullize(additionalFlagsField.getValue());
  }

  public boolean isShowInBrowser() {
    return myOpenInBrowserCheckBox.getValue();
  }

  public boolean isVerbose() {
    return myVerboseCheckBox.getValue();
  }

  public String getDocTitle() {
    return StringUtil.notNullize(docTitle.getValue());
  }

  public String getMaxHeapSize() {
    return StringUtil.notNullize(maxHeapSizeField.getValue());
  }

  public Component createCenterPanel() {
    return myPanel1;
  }

  public TextBox getOutputDirChooser() {
    return destDirChooser.getComponent();
  }
}
