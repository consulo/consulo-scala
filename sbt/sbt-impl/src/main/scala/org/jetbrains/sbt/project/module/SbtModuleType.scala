package org.jetbrains.sbt
package project.module

import org.jetbrains.annotations.NonNls

// upstream: a ModuleType (in sbt-api) - Consulo has no module types, only the id is kept (it is passed to the module data of the import)
object SbtModuleType {
  @NonNls val Id = "SBT_MODULE"
}
