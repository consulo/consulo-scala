package org.jetbrains.sbt.language

import consulo.language.Language
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiManager
import consulo.language.impl.file.SingleRootFileViewProvider

private final class SbtFileViewProvider(
  manager: PsiManager,
  file: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language
) extends SingleRootFileViewProvider(manager, file, eventSystemEnabled, language)
