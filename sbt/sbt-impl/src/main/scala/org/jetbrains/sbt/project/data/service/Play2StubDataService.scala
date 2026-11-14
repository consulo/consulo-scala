package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.project.Project
import org.jetbrains.sbt.project.data.SbtPlay2ProjectData

@ExtensionImpl
class Play2StubDataService extends DefaultDataService[SbtPlay2ProjectData, Project](SbtPlay2ProjectData.Key)
