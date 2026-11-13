package org.jetbrains.plugins.scala.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;

import consulo.language.editor.folding.CodeFoldingSettings;
import com.intellij.java.language.codeInsight.folding.JavaCodeFoldingSettings;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;

@State(
        name = "ScalaCodeFoldingSettings",
        storages = {
                @Storage("editor.xml"),
                @Storage(value = "scala_folding_settings.xml", deprecated = true)
        }
)
@Singleton
@ServiceImpl
public class ScalaCodeFoldingSettingsImpl extends ScalaCodeFoldingSettings implements PersistentStateComponent<ScalaCodeFoldingSettingsImpl> {

  private boolean FOLD_ARGUMENT_BLOCK = false;
  private boolean FOLD_TEMPLATE_BODIES = false;
  private boolean FOLD_DEFINITION_BODIES = false;
  private boolean FOLD_BLOCK_COMMENTS = false;
  private boolean FOLD_PACKAGINGS = false;
  private boolean FOLD_TYPE_LAMBDA = false;
  private boolean FOLD_MULTILINE_STRING = false;
  private boolean FOLD_CUSTOM_REGION = false;
  private boolean FOLD_MULTILINE_BLOCKS = false;
  private boolean ADD_FOLDING_FOR_ALL_BLOCKS = false;

  public static ScalaCodeFoldingSettingsImpl getInstance() {
    return ApplicationManager.getApplication().getInstance(ScalaCodeFoldingSettingsImpl.class);
  }

  @Override
  public ScalaCodeFoldingSettingsImpl getState() {
    return this;
  }

  @Override
  public void loadState(ScalaCodeFoldingSettingsImpl scalaCodeFoldingSettings) {
    XmlSerializerUtil.copyBean(scalaCodeFoldingSettings, this);
  }

  @Override
  public boolean isCollapseFileHeaders() {
    return CodeFoldingSettings.getInstance().COLLAPSE_FILE_HEADER;
  }

  @Override
  public boolean isCollapseImports() {
    return  CodeFoldingSettings.getInstance().COLLAPSE_IMPORTS;
  }

  @Override
  public boolean isCollapseCustomRegions() {
    return FOLD_CUSTOM_REGION;
  }

  @Override
  public void setCollapseCustomRegions(boolean value) {
    FOLD_CUSTOM_REGION = value;
  }

  @Override
  public boolean isCollapseBlockComments() {
    return FOLD_BLOCK_COMMENTS;
  }

  @Override
  public void setCollapseBlockComments(boolean value) {
    FOLD_BLOCK_COMMENTS = value;
  }

  @Override
  public boolean isCollapseMethodCallBodies() {
    return FOLD_ARGUMENT_BLOCK;
  }

  @Override
  public void setCollapseMethodCallBodies(boolean value) {
    FOLD_ARGUMENT_BLOCK = value;
  }

  @Override
  public boolean isCollapseTemplateBodies() {
    return FOLD_TEMPLATE_BODIES;
  }

  @Override
  public void setCollapseTemplateBodies(boolean value) {
    FOLD_TEMPLATE_BODIES = value;
  }

  @Override
  public boolean isCollapseDefinitionWithAssignmentBodies() {
    return FOLD_DEFINITION_BODIES;
  }

  @Override
  public void setCollapseDefinitionWithAssignmentBodies(boolean value) {
    FOLD_DEFINITION_BODIES = value;
  }

  @Override
  public boolean isCollapseTypeLambdas() {
    return FOLD_TYPE_LAMBDA;
  }

  @Override
  public void setCollapseTypeLambdas(boolean value) {
    FOLD_TYPE_LAMBDA = value;
  }

  @Override
  public boolean isCollapsePackagings() {
    return FOLD_PACKAGINGS;
  }

  @Override
  public void setCollapsePackagings(boolean value) {
    FOLD_PACKAGINGS = value;
  }

  @Override
  public boolean isCollapseMultilineStrings() {
    return FOLD_MULTILINE_STRING;
  }

  @Override
  public void setCollapseMultilineStrings(boolean value) {
    FOLD_MULTILINE_STRING = value;
  }

  @Override
  public boolean isCollapseLineComments() {
    return JavaCodeFoldingSettings.getInstance().isCollapseEndOfLineComments();
  }

  @Override
  public boolean isCollapseScalaDocComments() {
    return CodeFoldingSettings.getInstance().COLLAPSE_DOC_COMMENTS;
  }

  @Override
  public boolean isCollapseMultilineBlocks() {
    return FOLD_MULTILINE_BLOCKS;
  }

  @Override
  public void setCollapseMultilineBlocks(boolean value) {
    FOLD_MULTILINE_BLOCKS = value;
  }

  @Override
  public boolean isFoldingForAllBlocks() {
    return ADD_FOLDING_FOR_ALL_BLOCKS;
  }

  @Override
  public void setFoldingForAllBlocks(boolean value) {
    ADD_FOLDING_FOR_ALL_BLOCKS = value;
  }
}
