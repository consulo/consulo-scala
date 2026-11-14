package org.jetbrains.plugins.scala.debugger.breakpoints;

import com.intellij.java.debugger.impl.breakpoints.properties.JavaLineBreakpointProperties;
import com.intellij.java.debugger.impl.ui.breakpoints.Breakpoint;
import com.intellij.java.debugger.impl.ui.breakpoints.JavaLineBreakpointType;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.project.Project;

/**
 * Scala cannot override {@link JavaLineBreakpointType#createJavaBreakpoint}: it takes a raw XBreakpoint while
 * the method of JavaBreakpointType takes a parameterized one.
 */
public abstract class ScalaLineBreakpointTypeBase extends JavaLineBreakpointType {
    protected ScalaLineBreakpointTypeBase(String id, String title) {
        super(id, title);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Breakpoint<JavaLineBreakpointProperties> createJavaBreakpoint(Project project, XBreakpoint breakpoint) {
        return createScalaBreakpoint(project, (XBreakpoint<JavaLineBreakpointProperties>) breakpoint);
    }

    protected abstract Breakpoint<JavaLineBreakpointProperties> createScalaBreakpoint(Project project, XBreakpoint<JavaLineBreakpointProperties> breakpoint);
}
