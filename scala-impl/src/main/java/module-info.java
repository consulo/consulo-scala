import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module org.intellij.scala {
    requires consulo.annotation;
    requires consulo.component.api;
    requires consulo.language.api;
    requires consulo.virtual.file.system.api;
    requires consulo.localize.api;
    requires consulo.ui.api;
    requires consulo.base.icon.library;

    requires scala.library;

    exports org.jetbrains.plugins.scala;
}
