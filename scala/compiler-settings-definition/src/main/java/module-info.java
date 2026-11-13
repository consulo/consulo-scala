import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
// open: persisted state classes are read and written reflectively by the Consulo XML serializer
open module org.intellij.scala.compiler.settings.definition {
    requires transitive scala.library;
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.component.api;
    requires consulo.util.xml.serializer;
    requires jakarta.inject;

    exports org.jetbrains.plugins.scala.compilersettings.settings;
}
