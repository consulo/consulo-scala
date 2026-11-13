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

    requires scala.library;

    requires org.intellij.scala.utils.language.rt;
    requires org.intellij.scala.utils.language;
    requires org.intellij.scala.tasty.reader;
    requires org.intellij.scala.decompiler;
    requires org.intellij.scala.api;

    exports org.jetbrains.plugins.scala;
}
