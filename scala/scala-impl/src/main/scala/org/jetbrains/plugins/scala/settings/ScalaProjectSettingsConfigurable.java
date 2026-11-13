package org.jetbrains.plugins.scala.settings;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider$;

import javax.swing.*;

public class ScalaProjectSettingsConfigurable implements Configurable {

  public ScalaProjectSettingsConfigurable(Project project) {
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Scala";
  }

  @Override
  public String getHelpTopic() {
    return ScalaWebHelpProvider$.MODULE$.HelpPrefix() + "scala-features-overview-scala.html";
  }

  @Override
  public JComponent createComponent() {
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
