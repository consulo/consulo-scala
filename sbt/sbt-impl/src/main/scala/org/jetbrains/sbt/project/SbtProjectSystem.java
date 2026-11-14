package org.jetbrains.sbt.project;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import org.jetbrains.sbt.Sbt;

// upstream: lives in sbt-api
public final class SbtProjectSystem {
    public static final ProjectSystemId Id = new ProjectSystemId("SBT", LocalizeValue.of(Sbt.Name()), Sbt.Icon(), Sbt.FolderIcon());
}
