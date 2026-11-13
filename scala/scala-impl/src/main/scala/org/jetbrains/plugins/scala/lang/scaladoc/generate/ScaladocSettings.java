package org.jetbrains.plugins.scala.lang.scaladoc.generate;

import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;


@State(
        name = "ScaladocSettings",
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ScaladocSettings implements PersistentStateComponent<ScaladocSettings> {

  public String outputDir = null;
  public String additionalFlags = null;
  public String maxHeapSize = null;
  public String docTitle = null;
  public Boolean verbose = null;
  public Boolean openInBrowser = null;

  @Override
  public ScaladocSettings getState() {
    return this;
  }

  @Override
  public void loadState(ScaladocSettings state) {
    XmlSerializerUtil.copyBean(state, this); // TODO: from docs:  defensive copying is not required.
  }

  public static ScaladocSettings getInstance(Project project) {
    return project.getInstance(ScaladocSettings.class);
  }

}
