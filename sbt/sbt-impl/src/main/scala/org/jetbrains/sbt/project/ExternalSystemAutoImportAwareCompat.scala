package org.jetbrains.sbt.project

import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware
import consulo.project.Project

import java.nio.file.Path

/**
 * A compatibility trait which exists to make sure that the new [[java.nio.file.Path]] based API is
 * abstract and must be overridden.
 */
// upstream: also implements the deprecated java.io.File based getAffectedExternalProjectFiles (not in Consulo)
trait ExternalSystemAutoImportAwareCompat extends ExternalSystemAutoImportAware:
  override def getAffectedExternalProjectFilePaths(projectPath: String, project: Project): java.util.List[Path]
