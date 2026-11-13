package org.jetbrains.plugins.scala.findUsages.factory

import com.intellij.java.analysis.impl.find.findUsages.JavaFindUsagesOptions
import consulo.component.PropertiesComponent
import consulo.project.Project

// upstream: @ApiStatus.Internal
abstract class ScalaFindUsagesOptionsBase(project: Project, isSearchForTextOccurrencesDefault: Boolean = true) extends JavaFindUsagesOptions(project) {
  //NOTE: this is the field from base class
  isSearchForTextOccurrences = isSearchForTextOccurrencesDefault

  // upstream: overrides of FindUsagesOptions.storeDefaults/setDefaults (no such methods in Consulo FindUsagesOptions)
  def storeDefaults(properties: PropertiesComponent, prefix: String): Unit = {
    properties.setValue(prefix + "isSearchForTextOccurrences", isSearchForTextOccurrences, isSearchForTextOccurrencesDefault)
  }

  def setDefaults(properties: PropertiesComponent, prefix: String): Unit = {
    isSearchForTextOccurrences = properties.getBoolean(prefix + "isSearchForTextOccurrences", isSearchForTextOccurrencesDefault)
  }
}
