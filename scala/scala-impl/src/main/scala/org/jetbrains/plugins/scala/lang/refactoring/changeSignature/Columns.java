package org.jetbrains.plugins.scala.lang.refactoring.changeSignature;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.changeSignature.ParameterInfo;
import consulo.language.editor.refactoring.changeSignature.ParameterTableModelBase;
import consulo.language.editor.refactoring.changeSignature.ParameterTableModelItemBase;
import consulo.ui.ex.awt.ColumnInfo;

//sole purpose of this class is to expose protected static classes of ParameterTableModelBase
public abstract class Columns extends ParameterTableModelBase {

  public Columns(PsiElement typeContext, PsiElement defaultValueContext, ColumnInfo[] columnInfos) {
    super(typeContext, defaultValueContext, columnInfos);
  }

  public static class NameColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>>
          extends ParameterTableModelBase.NameColumn<P, TableItem> {

    public NameColumn(Project project) {
      super(project);
    }
  }

  public static class TypeColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>>
          extends ParameterTableModelBase.TypeColumn<P, TableItem> {

    public TypeColumn(Project project, FileType fileType) {
      super(project, fileType);
    }
  }

  public static class DefaultValueColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>>
          extends ParameterTableModelBase.DefaultValueColumn<P, TableItem> {

    public DefaultValueColumn(Project project, FileType fileType) {
      super(project, fileType);
    }
  }
}
