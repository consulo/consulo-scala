package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtensionProvider
import org.jetbrains.bsp.BSP

// upstream: BspSyntheticModuleType (a ModuleType with a module configuration editor provider: content/output/classpath
// editors) - Consulo has no module types
/**
 * The module extension of modules imported from BSP (Consulo requires one per external system).
 */
@ExtensionImpl
final class BspModuleExtensionProvider extends ExternalSystemModuleExtensionProvider(BSP.ProjectSystemId) {
  override def getParentId: String = "java"
}
