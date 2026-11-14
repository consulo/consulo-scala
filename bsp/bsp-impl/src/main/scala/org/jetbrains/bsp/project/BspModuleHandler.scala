package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl

import consulo.module.Module
import org.jetbrains.bsp.BspUtil
import org.jetbrains.sbt.project.BuildToolModuleHandler

//noinspection ApiStatus
@ExtensionImpl
private final class BspModuleHandler extends BuildToolModuleHandler {
  override def handles(module: Module): Boolean = BspUtil.isBspModule(module)
}
