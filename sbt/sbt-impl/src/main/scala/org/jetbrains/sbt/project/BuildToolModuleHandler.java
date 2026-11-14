package org.jetbrains.sbt.project;

import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;

// upstream: @ApiStatus.Internal
public interface BuildToolModuleHandler {
    ExtensionPointName<BuildToolModuleHandler> EP_NAME = ExtensionPointName.create("org.jetbrains.sbt.buildToolModuleHandler");

    boolean handles(Module module);

    // upstream: @ApiStatus.Internal
    static boolean isHandledByBuildTool(Module module) {
        return EP_NAME.getExtensionList().stream().anyMatch(handler -> handler.handles(module));
    }
}
