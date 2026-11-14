package org.jetbrains.sbt.project.sources

import org.jetbrains.annotations.NonNls

// upstream: a ModuleType - Consulo has no module types, only the id is kept (it is passed to the module data of the import)
object SharedSourcesModuleType {
  @NonNls val Id = "SHARED_SOURCES_MODULE"
}
