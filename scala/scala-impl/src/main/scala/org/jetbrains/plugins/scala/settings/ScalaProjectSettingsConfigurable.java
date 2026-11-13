package org.jetbrains.plugins.scala.settings;

import consulo.annotation.component.ExtensionImpl;

import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import jakarta.inject.Inject;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider$;

@ExtensionImpl(id = ScalaProjectSettingsConfigurable.ID)
public class ScalaProjectSettingsConfigurable implements ProjectConfigurable {
  public static final String ID = "configurable.scala.root";

  @Inject
  public ScalaProjectSettingsConfigurable(Project project) {
  }

  @Override
  public String getId() {
    return ID;
  }

  // upstream: groupId="language" (Languages & Frameworks), Consulo has no such group
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
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
