import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
module org.intellij.scala.api {
    requires transitive scala.library;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.component.api;
    requires consulo.disposer.api;
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.localize.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.virtual.file.system.api;

    requires consulo.java.language.api;
    requires consulo.java.language.impl;

    requires jakarta.inject;

    exports org.jetbrains.plugins.scala.api;
    exports org.jetbrains.plugins.scala.api.extensions.implementation.iterator;
    exports org.jetbrains.plugins.scala.api.lang.psi;
    exports org.jetbrains.plugins.scala.api.lang.psi.api;
    exports org.jetbrains.plugins.scala.api.lang.psi.stubs;
    exports org.jetbrains.plugins.scala.api.project;
    exports org.jetbrains.plugins.scala.api.util;
    exports org.jetbrains.plugins.scala.icons;
}
