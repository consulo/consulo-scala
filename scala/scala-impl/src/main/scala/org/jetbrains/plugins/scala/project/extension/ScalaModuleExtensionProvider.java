package org.jetbrains.plugins.scala.project.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import org.jetbrains.plugins.scala.icons.Icons;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class ScalaModuleExtensionProvider implements ModuleExtensionProvider<ScalaModuleExtension> {
    @Override
    public String getId() {
        return "scala";
    }

    @Override
    public @Nullable String getParentId() {
        return "java";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Scala");
    }

    @Override
    public Image getIcon() {
        return Icons.SCALA_SDK;
    }

    @Override
    public ModuleExtension<ScalaModuleExtension> createImmutableExtension(ModuleRootLayer layer) {
        return new ScalaModuleExtension(getId(), layer);
    }

    @Override
    public MutableModuleExtension<ScalaModuleExtension> createMutableExtension(ModuleRootLayer layer) {
        return new ScalaMutableModuleExtension(getId(), layer);
    }
}
