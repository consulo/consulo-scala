package org.jetbrains.plugins.scala.lang.scaladoc.generate;

import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;


@State(
        name = "ScaladocSettings",
        reportStatistic = true
)
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
    return project.getService(ScaladocSettings.class);
  }

}
