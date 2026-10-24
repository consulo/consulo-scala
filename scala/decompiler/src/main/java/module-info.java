import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module org.intellij.scala.decompiler {
    requires transitive scala.library;
    requires scala.reflect;
    requires consulo.annotation;
    requires consulo.logging.api;
    requires consulo.util.lang;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires asm;

    requires org.intellij.scala.utils.language;

    exports org.jetbrains.plugins.scala.decompiler;
    exports org.jetbrains.plugins.scala.decompiler.scalasig;
}
