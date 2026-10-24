import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module org.intellij.scala.tasty.reader {
    requires transitive scala.library;
    requires consulo.annotation;

    requires org.intellij.scala.utils.language;
    requires org.intellij.scala.utils.language.rt;

    exports org.jetbrains.plugins.scala.tasty.reader;
}
