package org.jetbrains.plugins.scala.settings;

import consulo.annotation.component.ExtensionImpl;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider$;

@ExtensionImpl(id = "configurable.scala.root")
public class ScalaProjectSettingsConfigurable implements Configurable {

  public ScalaProjectSettingsConfigurable(Project project) {
  }

  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.of("Scala");
  }

  @Override
  public String getHelpTopic() {
    return ScalaWebHelpProvider$.MODULE$.HelpPrefix() + "scala-features-overview-scala.html";
  }

  @Override
  public Component createUIComponent(Disposable parentDisposable) {
    return null;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }
}
