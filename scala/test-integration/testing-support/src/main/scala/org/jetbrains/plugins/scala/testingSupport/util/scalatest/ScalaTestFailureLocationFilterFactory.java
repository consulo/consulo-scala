package org.jetbrains.plugins.scala.testingSupport.util.scalatest;

import consulo.annotation.component.ExtensionImpl;

import com.intellij.java.execution.filters.ExceptionFilterFactory;
import consulo.execution.ui.console.Filter;
import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;

import java.util.Objects;

@ExtensionImpl
public class ScalaTestFailureLocationFilterFactory implements ExceptionFilterFactory {
  @Override
  public Filter create(GlobalSearchScope searchScope) {
    return create(Objects.requireNonNull(searchScope.getProject()), searchScope);
  }

  public Filter create(Project project, GlobalSearchScope searchScope) {
    return new ScalaTestFailureLocationFilter(project, searchScope);
  }
}
