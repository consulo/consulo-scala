package org.jetbrains.bsp.project

import consulo.fileChooser.FileChooserDescriptor

// upstream: extends OpenProjectFileChooserDescriptor (internal in Consulo)
class BspOpenProjectDescriptor extends FileChooserDescriptor(true, true, false, false, false, false)
