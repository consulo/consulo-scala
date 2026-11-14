package org.jetbrains.plugins.scala.project.gradle

import consulo.annotation.component.ExtensionImpl
import consulo.gradle.GradleConstants
import org.jetbrains.plugins.scala.compiler.sync.ExternalSystemBasedProjectSyncHandler

@ExtensionImpl
final class GradleProjectSyncHelper extends ExternalSystemBasedProjectSyncHandler(GradleConstants.SYSTEM_ID)
