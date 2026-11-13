package org.jetbrains.plugins.scala.project

import consulo.annotation.component.ExtensionImpl

import consulo.virtualFileSystem.fileType.FileType
import consulo.language.file.LanguageFileType
import consulo.module.Module
import consulo.project.Project
import consulo.index.io.data.DataInputOutputUtil
import consulo.module.content.{FilePropertyPusher, ProjectFileIndex, PushedFilePropertiesUpdater}
import consulo.util.dataholder.Key
import consulo.virtualFileSystem.{FileAttribute, VirtualFile, VirtualFileWithId}
import consulo.language.psi.PsiFile
import consulo.language.psi.stub.IndexingDataKeys
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.lang.parser.ScalaLanguageSubstitutor
import org.jetbrains.plugins.scala.project.ScalaFeaturePusher.{SerializedScalaFeatures, isScalaLike}
import org.jetbrains.plugins.scala.project.ScalaFeatures.SerializableScalaFeatures

import java.io.IOException
import scala.util.Using

/**
 * Persists the effective [[ScalaFeatures]] for source directories
 * and exposes them to parser/highlighter code through file properties.
 *
 * The pusher stores module-derived feature flags on directories in source content.
 * Consumers can then read those features for a [[com.intellij.psi.PsiFile]] without resolving a module on every access.
 * When the pushed value changes, all Scala-like files in the affected directory are invalidated,
 * so syntax highlighting and parsing are recomputed with the new language feature set.
 *
 * Typical scenarios:
 *  - Scala 3 experimental capture checking should be parsed only when enabled in settings, while still working
 *    for Scala 3.8 standard-library sources loaded from source jars<br>
 *    (see [[https://youtrack.jetbrains.com/issue/SCL-24630 SCL-24630]])
 *  - Raw string unicode-escape behavior differs between Scala 2 and Scala 3,
 *    so lexer/parser consumers must read the pushed feature flags instead of assuming one global behavior<br>
 *    (see [[https://youtrack.jetbrains.com/issue/SCL-18631 SCL-18631]]).
 *
 * @see [[ScalaFeatures]]
 * @see [[com.intellij.openapi.roots.impl.JavaLanguageLevelPusher]]
 */
//noinspection UnstableApiUsage,ApiStatus
// upstream: extends FilePropertyPusherBase with a persistent FilePropertyKey
@ExtensionImpl
class ScalaFeaturePusher extends FilePropertyPusher[SerializedScalaFeatures] {

  override def persistAttribute(project: Project, fileOrDir: VirtualFile, value: SerializedScalaFeatures): Unit = {
    if (value == null || ScalaFeaturePusher.readPersistentValue(fileOrDir).contains(value.intValue))
      return

    ScalaFeaturePusher.writePersistentValue(fileOrDir, value)

    val updater = PushedFilePropertiesUpdater.getInstance(project)
    updater.filePropertiesChanged(fileOrDir, isScalaLike(_))
    fileOrDir.getChildren
      .iterator
      .filter(c => !c.isDirectory && isScalaLike(c))
      .foreach(updater.filePropertiesChanged(_))
  }

  override def getFileDataKey: Key[SerializedScalaFeatures] = ScalaFeaturePusher.key

  override def pushDirectoriesOnly(): Boolean = true

  override def getDefaultValue: SerializedScalaFeatures = ScalaFeatures.default.serializeToInt

  override def getImmediateValue(module: Module): SerializedScalaFeatures = module.features.serializeToInt

  override def getImmediateValue(project: Project, file: VirtualFile): SerializedScalaFeatures = null

  override def acceptsDirectory(file: VirtualFile, project: Project): Boolean =
    ProjectFileIndex.getInstance(project).isInSourceContent(file)

  override def acceptsFile(file: VirtualFile, project: Project): Boolean = false
}

object ScalaFeaturePusher {
  type SerializedScalaFeatures = Integer

  /**
   * @param file can represent a file or a directory
   * @note if it can't detect features from PsiFile, it fall backs from the features from VirtualFile (if exist).
   *       Q: Though maybe it could do the opposite? First try teh virtual file and if there are not features there, find the file?
   */
  def getFeatures(file: PsiFile): Option[ScalaFeatures] = {
    val containingDir = Option(file.getContainingDirectory)
    val featuresPersistedInContainingDir: Option[ScalaFeatures] =
      containingDir.flatMap(dir => getPersistedFeatures(dir.getVirtualFile))

    def fromIndexedDirOrContainingDir: Option[ScalaFeatures] = {
      val indexedVFile = Option(file.getUserData(IndexingDataKeys.VIRTUAL_FILE))
      // while indexing, the parser will get a dummy file that only references the real file
      val dir = indexedVFile.flatMap(vFile => if (vFile.isDirectory) Some(vFile) else Option(vFile.getParent))
      dir.flatMap(getPersistedFeatures)
    }

    // Original commit: [cc] force scala 3 stdlib source to support capture checking #SCL-24630
    def fromVirtualFile(vFile: VirtualFile): Option[ScalaFeatures] = {
      getPersistedFeatures(vFile).orElse {
        // Original commit: [cc] parse capture checking only if it is enabled by settings #SCL-24630
        // todo: this is a quick hack for idea253 release
        //       it should be improved by having a comprehensive return value from ScalaLanguageSubstitutor
        val path = vFile.getPath
        val isIn3_8StdLibSource =
          ScalaLanguageSubstitutor.isInSourceJar(path) &&
            ScalaLanguageSubstitutor.looksLikeScala3LibSourcesJar(path)
        Option.when(isIn3_8StdLibSource)(
          ScalaFeatures.onlyByVersion(ScalaVersion.Latest.Scala_3_8)
            .copy(ScalaVersion.Latest.Scala_3_8, hasCaptureCheckingEnabled = true)
        )
      }
    }

    val fromPsi = featuresPersistedInContainingDir.orElse(fromIndexedDirOrContainingDir)
    fromPsi.orElse {
      val virtualFile = Option(file.getVirtualFile)
      virtualFile.flatMap(fromVirtualFile)
    }
  }

  private def getPersistedFeatures(dir: VirtualFile): Option[ScalaFeatures] = {
    val persisted = Option(dir.getUserData(key)).map(_.intValue).orElse(readPersistentValue(dir))
    persisted.map(ScalaFeatures.deserializeFromInt(_))
  }

  //TODO: don't use it. Use approach similar to org.jetbrains.plugins.scala.lang.lexer.highlightingLexer.ScalaHighlightingLexerTestBase.configureModuleScalaVersionAndAdditionalCompilerOptions
  // It's closer to the production code
  @TestOnly
  def setFeatures(dir: VirtualFile, features: SerializableScalaFeatures): Unit =
    writePersistentValue(dir, features.serializeToInt)

  @inline
  private def isScalaLike(file: VirtualFile): Boolean =
    isScalaLike(file.getFileType)

  private def isScalaLike(fileType: FileType): Boolean =
    fileType match {
      case lft: LanguageFileType => lft.getLanguage.isKindOf(ScalaLanguage.INSTANCE)
      case _                     => false
    }

  private val key: Key[SerializedScalaFeatures] = Key.create("Pushed Scala Features")

  private val persistence = new FileAttribute("scala_pushed_feature_persistence", ScalaFeatures.version, true)

  // file attributes exist only for files of the persistent VFS (not for LightVirtualFile etc.)
  private def readPersistentValue(fileOrDir: VirtualFile): Option[Int] =
    if (!fileOrDir.isInstanceOf[VirtualFileWithId]) None
    else try {
      Option(persistence.readAttribute(fileOrDir)).flatMap { stream =>
        Using.resource(stream)(s => Some(DataInputOutputUtil.readINT(s)))
      }
    } catch {
      case _: IOException => None
    }

  private def writePersistentValue(fileOrDir: VirtualFile, value: Int): Unit =
    if (fileOrDir.isInstanceOf[VirtualFileWithId])
      Using.resource(persistence.writeAttribute(fileOrDir))(DataInputOutputUtil.writeINT(_, value))
}
