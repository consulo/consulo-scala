package org.jetbrains.plugins.scala.uiTests.backend;

import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting()
public final class ScalaSdkUtil {
    private ScalaSdkUtil() {}

    @VisibleForTesting()
    public static void setupScalaSdk(String scalaVersion) {
        ScalaSdkUtilImpl.setupScalaSdk(scalaVersion);
        System.out.println("Scala SDK with version " + scalaVersion + " is set up");
    }
}
