import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
module org.intellij.scala.sbt.api {
    requires transitive scala.library;
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.component.api;
    requires consulo.container.api;
    requires consulo.language.api;
    requires consulo.module.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.virtual.file.system.api;
    requires transitive org.intellij.scala.api;
    requires org.intellij.scala.utils.language.rt;

    exports org.jetbrains.sbt.api;
    exports org.jetbrains.sbt.api.icons;
    exports org.jetbrains.sbt.api.language;
    exports org.jetbrains.sbt.api.resolvers;
    exports org.jetbrains.sbt.api.project;
}
