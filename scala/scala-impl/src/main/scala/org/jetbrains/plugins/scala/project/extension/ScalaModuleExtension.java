package org.jetbrains.plugins.scala.project.extension;

import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.module.extension.ModuleExtension;

/**
 * Enables Scala in a module (Consulo only, upstream modules have Scala when they have a Scala SDK library).
 * The Scala SDK library still provides the Scala version and compiler.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class ScalaModuleExtension extends ModuleExtensionBase<ScalaModuleExtension> implements ModuleExtension<ScalaModuleExtension> {
    public ScalaModuleExtension(String id, ModuleRootLayer layer) {
        super(id, layer);
    }
}
