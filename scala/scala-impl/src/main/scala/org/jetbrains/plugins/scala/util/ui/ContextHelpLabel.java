package org.jetbrains.plugins.scala.util.ui;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBLabel;

/**
 * Replacement of IntelliJ {@code com.intellij.ui.ContextHelpLabel}: a help icon showing the description as tooltip.
 */
public final class ContextHelpLabel extends JBLabel {
    private ContextHelpLabel(String description) {
        super(PlatformIconGroup.generalContexthelp());
        setToolTipText(description);
    }

    public static ContextHelpLabel create(String description) {
        return new ContextHelpLabel(description);
    }

    @RequiredUIAccess
    public static Label createLabel(String description) {
        Label label = Label.create();
        label.setImage(PlatformIconGroup.generalContexthelp());
        label.setToolTipText(LocalizeValue.of(description));
        return label;
    }
}
