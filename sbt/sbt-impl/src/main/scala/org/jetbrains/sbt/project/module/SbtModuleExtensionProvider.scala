package org.jetbrains.sbt.project.module

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.service.module.extension.{ExternalSystemModuleExtensionImpl, ExternalSystemModuleExtensionProvider}
import consulo.module.content.layer.ModuleRootLayer
import consulo.module.extension.MutableModuleExtension
import org.jetbrains.sbt.project.SbtProjectSystem

/**
 * The module extension of modules imported from sbt (Consulo requires one per external system).
 */
@ExtensionImpl
final class SbtModuleExtensionProvider extends ExternalSystemModuleExtensionProvider(SbtProjectSystem.Id) {
  override def getParentId: String = "java"

  override def createMutableExtension(moduleRootLayer: ModuleRootLayer): MutableModuleExtension[ExternalSystemModuleExtensionImpl] =
    new SbtMutableModuleExtension(getId, moduleRootLayer)
}
