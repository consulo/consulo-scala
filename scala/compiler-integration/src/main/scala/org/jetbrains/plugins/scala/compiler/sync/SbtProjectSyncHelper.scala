package org.jetbrains.plugins.scala.compiler.sync

import consulo.annotation.component.ExtensionImpl
import org.jetbrains.sbt.project.SbtProjectSystem

@ExtensionImpl
final class SbtProjectSyncHelper extends ExternalSystemBasedProjectSyncHandler(SbtProjectSystem.Id)
