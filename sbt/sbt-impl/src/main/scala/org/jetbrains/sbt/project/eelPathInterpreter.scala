//noinspection ApiStatus,UnstableApiUsage
package org.jetbrains.sbt.project

import consulo.platform.Platform
import org.jetbrains.sbt.project.structure.data.{InterpretablePath, PathInterpreter}

import java.nio.file.Path

// upstream: EEL API (EelDescriptor, EelPath) - Consulo Platform API is used instead
extension (path: InterpretablePath)
  def toPath(using Platform): Path =
    val interpreter = summon[PathInterpreter[Path]]
    interpreter.interpret(path)

given eelPathInterpreter: (platform: Platform) => PathInterpreter[Path] = new PathInterpreter[Path]:
  override def interpret(path: InterpretablePath): Path =
    platform.fs().getPath(path.unsafePathString)
