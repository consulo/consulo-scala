import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
module org.intellij.scala.compiler.shared {
    requires transitive scala.library;
    requires consulo.annotation;
    requires consulo.component.api;
    requires consulo.logging.api;
    requires consulo.util.collection;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.util.xml.serializer;
    requires transitive org.intellij.scala.utils.language.rt;

    exports org.jetbrains.jps.incremental.scala.shared;
    exports org.jetbrains.jps.incremental.scala.shared.remote;
    exports org.jetbrains.jps.incremental.scala.tracing;
    exports org.jetbrains.jps.incremental.scala.utils;
    exports org.jetbrains.plugins.scala.compiler.buildinfo;
    exports org.jetbrains.plugins.scala.compiler.data;
    exports org.jetbrains.plugins.scala.compiler.data.serialization;
    exports org.jetbrains.plugins.scala.compiler.data.worksheet;
    exports org.jetbrains.plugins.scala.compiler.diagnostics;
    exports org.jetbrains.plugins.scala.server;
    exports org.jetbrains.plugins.scala.shared.compiler;
    exports org.jetbrains.plugins.scala.shared.compiler.references;
    exports org.jetbrains.plugins.scala.shared.project;
    exports org.jetbrains.plugins.scala.shared.util;
}
