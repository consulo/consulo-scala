package org.jetbrains.sbt

import consulo.annotation.component.ExtensionImpl

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import org.jetbrains.sbt.api.icons.Icons

import javax.swing.Icon

@ExtensionImpl
class SbtIconProvider extends ExternalSystemIconProvider {
  override def getReloadIcon: Icon = Icons.SBT_LOAD_CHANGES
}
