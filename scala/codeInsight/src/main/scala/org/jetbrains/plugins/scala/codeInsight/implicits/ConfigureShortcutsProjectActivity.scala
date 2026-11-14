package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.annotation.component.ExtensionImpl
import consulo.platform.Platform
import consulo.project.Project
import consulo.ui.ex.action.KeyboardShortcut
import consulo.ui.ex.keymap.KeymapManager
import org.jetbrains.plugins.scala.startup.ProjectActivity

import java.util.concurrent.atomic.AtomicBoolean

@ExtensionImpl
final class ConfigureShortcutsProjectActivity extends ProjectActivity {
  override def execute(project: Project): Unit = {
    // upstream: an application-level service whose constructor registers the shortcuts exactly once
    if (ConfigureShortcutsProjectActivity.Registered.compareAndSet(false, true)) {
      ConfigureShortcutsProjectActivity.registerShortcuts()
    }
  }
}

private object ConfigureShortcutsProjectActivity {
  private val Registered = new AtomicBoolean(false)

  private def registerShortcuts(): Unit = {
    val os = Platform.current().os()
    if (!os.isWindows && !os.isMac) { // Workaround for SCL-21346
      val keymap = KeymapManager.getInstance.getActiveKeymap
      keymap.removeShortcut("ZoomInIdeAction", KeyboardShortcut.fromString("shift control alt EQUALS"))
      keymap.removeShortcut("ZoomOutIdeAction", KeyboardShortcut.fromString("shift control alt MINUS"))
      keymap.removeShortcut("ResetIdeScaleAction", KeyboardShortcut.fromString("shift control alt 0"))
    }

    ImplicitShortcuts.setShortcuts(ShowImplicitHintsAction.Id, ImplicitShortcuts.EnableShortcuts)
  }
}
