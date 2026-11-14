package org.jetbrains.bsp;

import consulo.scala.bsp.icon.BspIconGroup;
import consulo.ui.image.Image;

public interface Icons {
    Image BSP = BspIconGroup.buildserverprotocol();
    Image BSP_TARGET = BspIconGroup.buildserverprotocoltarget();
    // upstream: refresh.svg in old UI, buildServerProtocolLoadChanges.svg in New UI (BSPIconMappings.json)
    Image BSP_LOAD_CHANGES = BspIconGroup.buildserverprotocolloadchanges();
    Image BSP_TOOLWINDOW = BspIconGroup.toolwindowbuildserverprotocol();
}
