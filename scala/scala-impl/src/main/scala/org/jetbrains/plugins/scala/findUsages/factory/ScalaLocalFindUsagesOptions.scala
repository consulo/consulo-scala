package org.jetbrains.plugins.scala.findUsages.factory

import consulo.project.Project

final class ScalaLocalFindUsagesOptions(project: Project)
  extends ScalaFindUsagesOptionsBase(project, isSearchForTextOccurrencesDefault = false)
