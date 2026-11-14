package org.jetbrains.plugins.scala.project.extension;

import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
public class ScalaMutableModuleExtension extends ScalaModuleExtension implements MutableModuleExtension<ScalaModuleExtension> {
    public ScalaMutableModuleExtension(String id, ModuleRootLayer layer) {
        super(id, layer);
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createConfigurationComponent(Disposable disposable, Runnable updateOnCheck) {
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myIsEnabled = enabled;
    }

    @Override
    public boolean isModified(ScalaModuleExtension extension) {
        return myIsEnabled != extension.isEnabled();
    }
}
