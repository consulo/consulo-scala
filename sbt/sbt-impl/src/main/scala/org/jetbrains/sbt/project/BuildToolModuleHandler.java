package org.jetbrains.sbt.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.module.Module;

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BuildToolModuleHandler {
    boolean handles(Module module);

    // upstream: @ApiStatus.Internal
    static boolean isHandledByBuildTool(Module module) {
        return Application.get().getExtensionPoint(BuildToolModuleHandler.class).anyMatchSafe(handler -> handler.handles(module));
    }
}
