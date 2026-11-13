package org.jetbrains.plugins.scala.shared.project;

import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Contains Scala SDK properties, which are used both in IDEA process and JPS process.<br>
 * Child classes are used in communication between IDEA and JPS
 */
// upstream: serialized by xmlb, Consulo: dsl-json (@CompiledJson on concrete state class)
public abstract class ScalaLibraryPropertiesStateSharedInIdeaAndJps {

    // upstream: @Tag("compiler-classpath") @XCollection(elementName = "root", valueAttributeName = "url")
    protected final String[] compilerClasspath;

    // upstream: @Tag("compiler-bridge-binary-jar")
    @Nullable
    protected final String compilerBridgeBinaryJar;

    // upstream: @Tag("repl-classpath") @XCollection(elementName = Constants.ENTRY, valueAttributeName = "url")
    protected final String[] replClasspath;

    public ScalaLibraryPropertiesStateSharedInIdeaAndJps() {
        this(ArrayUtil.EMPTY_STRING_ARRAY, null, ArrayUtil.EMPTY_STRING_ARRAY);
    }

    public ScalaLibraryPropertiesStateSharedInIdeaAndJps(
            String[] compilerClasspath,
            @Nullable String compilerBridgeBinaryJar,
            String[] replClasspath
    ) {
        this.compilerClasspath = compilerClasspath;
        this.compilerBridgeBinaryJar = compilerBridgeBinaryJar;
        this.replClasspath = replClasspath;
    }


    public final String[] getCompilerClasspath() {
        return compilerClasspath;
    }

    @Nullable
    public final String getCompilerBridgeBinaryJar() {
        return compilerBridgeBinaryJar;
    }

    public final String[] getReplClasspath() {
        return replClasspath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScalaLibraryPropertiesStateSharedInIdeaAndJps that = (ScalaLibraryPropertiesStateSharedInIdeaAndJps) o;
        return Arrays.equals(compilerClasspath, that.compilerClasspath) &&
                Objects.equals(compilerBridgeBinaryJar, that.compilerBridgeBinaryJar) &&
                Arrays.equals(replClasspath, that.replClasspath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(compilerClasspath),
                compilerBridgeBinaryJar,
                Arrays.hashCode(replClasspath)
        );
    }
}