import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module org.intellij.scala.utils.language.rt {
    requires transitive scala.library;
    requires consulo.annotation;

    exports org.jetbrains.plugins.scala.rt.project;
    exports org.jetbrains.plugins.scala.rt.util;
}
