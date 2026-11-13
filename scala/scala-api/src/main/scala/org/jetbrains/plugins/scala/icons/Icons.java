package org.jetbrains.plugins.scala.icons;

import consulo.scala.api.icon.ScalaApiIconGroup;
import consulo.ui.image.Image;

/**
 * NOTE: this class contains references to the icons in the "Old UI".
 * When the "New UI" is enabled, an automatic mapping is done between old icons and new icons.
 * The mappings are defined in ScalaIconMappings.json
 *
 * NOTE: File names must be in sync with [[https://github.com/JetBrains/IntelliJIcons]]
 *
 * @see com.intellij.icons.AllIcons
 */
public interface Icons {

    Image COMPILE_SERVER = ScalaApiIconGroup.compileserver();
    Image COMPILATION_CHARTS = ScalaApiIconGroup.compilationcharts();

    Image SCALA_FILE = ScalaApiIconGroup.filescala();
    Image MILL_FILE = ScalaApiIconGroup.millfile();

    Image ADD_CLAUSE = ScalaApiIconGroup.addclause();
    Image REMOVE_CLAUSE = ScalaApiIconGroup.removeclause();

    //SDK configuration
    Image SCALA_SDK = ScalaApiIconGroup.scalasdk();

    //Toplevel nodes
    Image CLASS = ScalaApiIconGroup.classscala();
    Image OBJECT = ScalaApiIconGroup.objectscala();
    Image CASE_CLASS = CLASS;
    Image CASE_OBJECT = OBJECT;
    Image ABSTRACT_CLASS = ScalaApiIconGroup.abstractclassscala();
    Image TRAIT = ScalaApiIconGroup.traitscala();
    Image ENUM = ScalaApiIconGroup.enumscala();
    Image PACKAGE_OBJECT = ScalaApiIconGroup.packageobject();

    //Mixed definition + companion object
    Image CLASS_AND_OBJECT = ScalaApiIconGroup.classobjectscala();
    Image ABSTRACT_CLASS_AND_OBJECT = ScalaApiIconGroup.abstractclassobjectscala();
    Image TRAIT_AND_OBJECT = ScalaApiIconGroup.traitobjectscala();
    Image ENUM_AND_OBJECT = ScalaApiIconGroup.enumobjectscala();
    Image TYPE_AND_OBJECT = ScalaApiIconGroup.typeobjectscala();

    //Companion gutter icons
    Image CLASS_COMPANION = ScalaApiIconGroup.classcompanion();
    Image CLASS_COMPANION_SWAPPED = ScalaApiIconGroup.classcompanionswapped();
    Image TRAIT_COMPANION = ScalaApiIconGroup.traitcompanion();
    Image TRAIT_COMPANION_SWAPPED = ScalaApiIconGroup.traitcompanionswapped();
    Image TYPE_COMPANION = ScalaApiIconGroup.typecompanion();
    Image TYPE_COMPANION_SWAPPED = ScalaApiIconGroup.typecompanionswapped();
    Image OBJECT_COMPANION = ScalaApiIconGroup.objectcompanion();
    Image OBJECT_COMPANION_SWAPPED = ScalaApiIconGroup.objectcompanionswapped();

    //Internal nodes
    Image FIELD_VAR = ScalaApiIconGroup.fieldvariable();
    Image ABSTRACT_FIELD_VAR = ScalaApiIconGroup.abstractfieldvariable();
    Image FIELD_VAL = ScalaApiIconGroup.fieldvalue();
    Image ABSTRACT_FIELD_VAL = ScalaApiIconGroup.abstractfieldvalue();
    Image TYPE_ALIAS = ScalaApiIconGroup.typealias();
    Image ABSTRACT_TYPE_ALIAS = ScalaApiIconGroup.abstracttypealias();
    Image FUNCTION = ScalaApiIconGroup.function();
    Image EXTENSION = FUNCTION; // TODO: need dedicated icon or not?
    Image VAR = ScalaApiIconGroup.variable();
    Image VAL = ScalaApiIconGroup.value();
    Image PARAMETER = ScalaApiIconGroup.parameter();
    Image PATTERN_VAL = ScalaApiIconGroup.patternvalue();
    Image LAMBDA = ScalaApiIconGroup.lambda();
    Image RECURSION = ScalaApiIconGroup.recursion();
    Image TAIL_RECURSION = ScalaApiIconGroup.tailrecursion();

    //Testing support
    Image SCALA_TEST = ScalaApiIconGroup.scalatesticon();
    Image SCALA_TEST_NODE = ScalaApiIconGroup.test();

    //Console
    Image SCALA_CONSOLE = ScalaApiIconGroup.scalaconsole();

    // Highlighting mode (status bar)
    Image HIGHLIGHTING_MODE = ScalaApiIconGroup.typed();

    // X-Ray mode widget
    Image PIN = ScalaApiIconGroup.pin();
}
