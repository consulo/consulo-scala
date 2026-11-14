package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.annotation.component.ExtensionImpl

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import com.intellij.openapi.actionSystem.KeyboardShortcut
import consulo.application.ApplicationManager
import com.intellij.openapi.client.ClientSystemInfo
import com.intellij.openapi.keymap.KeymapManager
import consulo.project.Project
import org.jetbrains.plugins.scala.startup.ProjectActivity

import javax.swing.KeyStroke

@ExtensionImpl
private final class ConfigureShortcutsProjectActivity extends ProjectActivity {
  override def execute(project: Project): Unit = {
    // Application-level services are initialised exactly once.
    ApplicationManager.getApplication.getInstance(classOf[ConfigureShortcutsProjectActivity.AppService])
  }
}

private object ConfigureShortcutsProjectActivity {
  // upstream: @Service(Array(Service.Level.APP))
  @Singleton
  @ServiceAPI(ComponentScope.APPLICATION)
  @ServiceImpl
  private final class AppService {
    // Executed in the service constructor.
    registerShortcuts()

    private def registerShortcuts(): Unit = {
      //noinspection ApiStatus,UnstableApiUsage
      if (!ClientSystemInfo.isWindows && !ClientSystemInfo.isMac) { // Workaround for SCL-21346
        val keymap = KeymapManager.getInstance.getActiveKeymap
        keymap.removeShortcut("ZoomInIdeAction", new KeyboardShortcut(KeyStroke.getKeyStroke("shift control alt EQUALS"), null))
        keymap.removeShortcut("ZoomOutIdeAction", new KeyboardShortcut(KeyStroke.getKeyStroke("shift control alt MINUS"), null))
        keymap.removeShortcut("ResetIdeScaleAction", new KeyboardShortcut(KeyStroke.getKeyStroke("shift control alt 0"), null))
      }

      ImplicitShortcuts.setShortcuts(ShowImplicitHintsAction.Id, ImplicitShortcuts.EnableShortcuts)
    }
  }
}
