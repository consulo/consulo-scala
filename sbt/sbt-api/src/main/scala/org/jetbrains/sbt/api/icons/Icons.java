package org.jetbrains.sbt.api.icons;

import consulo.sbt.api.icon.SbtApiIconGroup;
import consulo.ui.image.Image;

public interface Icons {
    Image SBT = SbtApiIconGroup.sbticon();
    Image SBT_FOLDER = SbtApiIconGroup.sbtfolder();
    Image SBT_FILE = SbtApiIconGroup.sbtfile();
    Image SBT_SHELL = SbtApiIconGroup.sbtshell();
    Image SBT_LOAD_CHANGES = SbtApiIconGroup.sbtloadchanges();
    Image SBT_GENERATE_MANAGED_SOURCES = SbtApiIconGroup.sbtgeneratemanagedsources();

    @SuppressWarnings("unused") // used from SBT.xml
    Image SBT_TOOL_WINDOW = SbtApiIconGroup.sbttoolwin();
    Image SBT_SHELL_TOOL_WINDOW = SbtApiIconGroup.sbtshelltoolwin();

    // upstream: findIconNotNull(path) - IconLoader.findIcon() for @20x20 tool window variants (IDEA-338105),
    // not applicable for Consulo icon library
}
