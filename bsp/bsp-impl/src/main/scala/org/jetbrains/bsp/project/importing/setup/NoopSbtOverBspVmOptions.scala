package org.jetbrains.bsp.project.importing.setup

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton

/** The production implementation of [[SbtOverBspVmOptions]], which adds no options */
@Singleton
@ServiceImpl
final class NoopSbtOverBspVmOptions extends SbtOverBspVmOptions:
  override def vmOptions: Seq[String] = Seq.empty
