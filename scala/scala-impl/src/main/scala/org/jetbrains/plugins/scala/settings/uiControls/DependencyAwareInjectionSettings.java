package org.jetbrains.plugins.scala.settings.uiControls;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings;

import javax.swing.*;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DependencyAwareInjectionSettings {
  public static ExtensionPointName<DependencyAwareInjectionSettings> EP_NAME = ExtensionPointName.create(DependencyAwareInjectionSettings.class);

  public abstract ComponentWithSettings createComponent(JPanel uiPlace);
  
  public abstract String getName();
  
  public interface ComponentWithSettings {
    void loadSettings(ScalaProjectSettings settings);
    void saveSettings(ScalaProjectSettings settings);
    boolean isModified(ScalaProjectSettings settings);
  }
}
