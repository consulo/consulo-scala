package org.jetbrains.plugins.scala.compiler.highlighting.services.requests

import consulo.document.Document
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.util.DocumentUtil
import org.jetbrains.plugins.scala.shared.util.{CanonicalPath, DocumentVersion}

import scala.collection.immutable.HashMap

type SerializableMap[K, V] = Map[K, V] & Serializable

/**
 * Contract for a compilation request handled by the scheduler.
 *
 * upstream: also incremental (JPS/BSP) compilations of the unbuilt files, here every request compiles one document
 * against the module output, see [[org.jetbrains.plugins.scala.compiler.highlighting.services.CompilerHighlightingService]]
 */
trait CompilationRequest:
  /** The file this request compiles, mapped to its open document. */
  def originFiles: Map[VirtualFile, Document]

  /** Snapshot of each file's document version, used to detect edits made after enqueueing. */
  final lazy val documentVersions: Map[VirtualFile, DocumentVersion] =
    originFiles.map { case (vf, doc) => vf -> DocumentUtil.documentVersion(vf, doc) }

  /** Whether a file was edited after the request was created, the compilation would be for an outdated text */
  def isExpired: Boolean =
    originFiles.keys.exists(!_.isValid) ||
      documentVersions.exists { case (virtualFile, version) =>
        version.version != DocumentUtil.version(originFiles(virtualFile))
      }

  /**
   * Executes the compilation on the calling thread.
   */
  private[services] def execute(): Unit

  /**
   * The text this compilation compiles: the document as it is in memory, which is what the document compiler reads.
   */
  protected final def reportedDocumentVersions: SerializableMap[CanonicalPath, Long] =
    documentVersions.map { case (_, DocumentVersion(path, version)) =>
      path -> version
    }.to(HashMap.mapFactory[CanonicalPath, Long])
