package org.jetbrains.plugins.scala.util.ui;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;

/**
 * Replacement of IntelliJ {@code com.intellij.ui.ContextHelpLabel}: a help icon showing the description as tooltip.
 */
public final class ContextHelpLabel extends JBLabel {
    private ContextHelpLabel(String description) {
        super(TargetAWT.to(PlatformIconGroup.generalContexthelp()));
        setToolTipText(description);
    }

    public static ContextHelpLabel create(String description) {
        return new ContextHelpLabel(description);
    }
}
