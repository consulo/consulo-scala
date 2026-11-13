package org.jetbrains.plugins.scala.uiTests.backend;


// upstream: @VisibleForTesting()
public final class ScalaSdkUtil {
    private ScalaSdkUtil() {}

    // upstream: @VisibleForTesting()
    public static void setupScalaSdk(String scalaVersion) {
        ScalaSdkUtilImpl.setupScalaSdk(scalaVersion);
        System.out.println("Scala SDK with version " + scalaVersion + " is set up");
    }
}
