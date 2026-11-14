/**
 * @author VISTALL
 * @since 2026-11-14
 */
open module org.intellij.scala.plugin {
    requires transitive org.intellij.scala;
    requires transitive org.intellij.scala.codeInsight;
    requires transitive org.intellij.scala.structure.view;
    requires transitive org.intellij.scala.conversion;
    requires transitive org.intellij.scala.scalastyle;
    requires transitive org.intellij.scala.meta;
    requires transitive org.intellij.scala.copyright;
    requires transitive org.intellij.scala.intellilang;
    requires transitive org.intellij.scala.repl;
    requires transitive org.intellij.scala.compiler.integration.server.management;
    requires transitive org.intellij.scala.compile.server;
    requires transitive org.intellij.scala.debugger;
    requires transitive org.intellij.scala.compiler.consulo;
    requires transitive org.intellij.scala.sbt.impl;
}
