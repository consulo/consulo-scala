package org.jetbrains.sbt.project.module

import consulo.externalSystem.model.{Key, ProjectKeys}
import consulo.externalSystem.model.project.ModuleData
import org.jetbrains.sbt.project.SbtProjectSystem

/**
 * This data class was created to represent main and test modules.
 * Main and test modules might be located both inside ModuleData modules (cause root module inside each build also can
 * contain main and test sources) and SbtNestedModuleData modules. The reason for the creation of this data class is
 * the same as it was for SbtNestedModuleData - adjusting proper module names.
 * See [[org.jetbrains.sbt.project.module.SbtNestedModuleData]] for more details
 *
 * @param id unique project id
 * @param externalName module external name
 * @param moduleFileDirectoryPath path to the directory in which .iml file for module is stored
 * @param externalConfigPath module path
 * @param moduleTypeId module type id
 */
// upstream: lives in sbt-api; Consulo has no module types (moduleTypeId is kept for the callers only)
// and no ModuleNameDeduplicationStrategy
final case class SbtSourceSetData(
  id: String,
  externalName: String,
  moduleFileDirectoryPath: String,
  externalConfigPath: String,
  moduleTypeId: String
) extends ModuleData(
  id,
  SbtProjectSystem.Id,
  externalName,
  moduleFileDirectoryPath,
  externalConfigPath
)

object SbtSourceSetData {
  val Key: Key[SbtSourceSetData] =
    new Key(classOf[SbtSourceSetData].getName, ProjectKeys.MODULE.getProcessingWeight + 2)
}
