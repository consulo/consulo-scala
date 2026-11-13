import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
// IntelliJ APIs needed by ported code, which are not available in Consulo yet (temporary, ported from intellij-community)
module org.intellij.scala.intellij.compat {
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.code.editor.api;
    requires consulo.virtual.file.system.api;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.lang;

    exports com.intellij.codeInsight.intention.preview;
}
