package org.jetbrains.plugins.scala.components.libextensions

import consulo.container.boot.ContainerPathManager
import consulo.http.HttpRequests
import consulo.logging.Logger
import consulo.application.progress.ProgressIndicator
import consulo.project.Project
import consulo.content.library.LibraryTablesRegistrar
import consulo.virtualFileSystem.StandardFileSystems
import consulo.virtualFileSystem.archive.ArchiveVfsUtil
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.search.FilenameIndex
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.DependencyManagerBase.{DependencyDescription, IvyResolver, MavenResolver}
import org.jetbrains.plugins.scala.components.libextensions.LibraryExtensionsManager._
import org.jetbrains.plugins.scala.{ScalaBundle, extensions}
import org.jetbrains.sbt.api.resolvers.{SbtIvyResolver, SbtMavenResolver, SbtResolver}

import java.io.{IOException, InputStreamReader}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

class ExtensionDownloader(private val progress: ProgressIndicator, private val sbtResolvers: Set[SbtResolver])(implicit project: Project) {

  private val LOG         = Logger.getInstance(classOf[ExtensionDownloader])

  def getExtensionJars: Seq[Path] = {
    val jarsWithProps = findJarsWithProps()
    for {(jar, prop) <- jarsWithProps} {
      LOG.info(s"${jar.getPath} -> ${prop.artifact}")
    }
    val libraryProps = jarsWithProps.map(_._2).toSet
    val bundledProps = getPropsFromLocalIndex
    val jars  = getRemoteExtensions((libraryProps ++ bundledProps).toSeq)
    jars
  }

  private def getPropsFromLocalIndex: Seq[ExtensionProps] = {
    val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    val descriptions = table.getLibraries.flatMap { lib =>
      lib.getName.split(": ?") match {
        case Array("sbt", org, module, version, "jar") =>
          Some(DependencyDescription(org, module, version))
        case _ =>
          None
      }
    }
    descriptions.flatMap(BundledExtensionIndex.INDEX.get).toSeq
  }

  private def getRemoteExtensions(props: Seq[ExtensionProps]): Seq[Path] = {
    val (normal, withOverrides) = props.partition(_.urlOverride == null)
    downloadViaIvy(normal) ++ withOverrides.flatMap(downloadDirect)
  }

  private def downloadDirect(props: ExtensionProps): Option[Path] = {
    val downloadRoot = ContainerPathManager.get().getSystemDir.resolve("scala/extensionsCache")
    val fileName     = s"${Math.abs(props.hashCode())}.jar"
    val targetFile   = downloadRoot.resolve(fileName)
    if (Files.exists(targetFile) && Files.size(targetFile) > 0)
      return Some(targetFile)

    // upstream: DownloadableFileService
    progress.setText(ScalaBundle.message("downloading.url", props.urlOverride))
    val partFile = downloadRoot.resolve(s"$fileName.part")
    try {
      Files.createDirectories(downloadRoot)
      HttpRequests.request(props.urlOverride).saveToFile(partFile, progress)
      Files.move(partFile, targetFile)
      Some(targetFile)
    } catch {
      case e: IOException =>
        LOG.error(s"Failed to download extension from ${props.urlOverride}", e)
        Files.deleteIfExists(partFile)
        Files.deleteIfExists(targetFile)
        None
    }
  }

  private def downloadViaIvy(props: Seq[ExtensionProps]): Seq[Path] = {
    val ivyResolvers = sbtResolvers.toSeq.collect {
      case r: SbtMavenResolver => MavenResolver(r.name, r.root)
      case r: SbtIvyResolver if !r.isLocal => IvyResolver(r.name, r.root)
    }
    val deps = props.map(_.artifact.toDepDescription)
    val resolver = new IvyExtensionsResolver(ivyResolvers, progress)
    resolver.resolve(deps*).map(_.file)
  }

  private def findJarsWithProps(): Seq[(VirtualFile, ExtensionProps)] = {
    val jarFS = StandardFileSystems.jar()

    val vFiles: Iterable[VirtualFile] = extensions.inReadAction {
      FilenameIndex.getVirtualFilesByName(project, PROPS_NAME, GlobalSearchScope.allScope(project)).asScala
    }
    val containingJars = vFiles.collect {
      case f if f.getFileSystem == jarFS =>
        ArchiveVfsUtil.getArchiveRootForLocalFile(ArchiveVfsUtil.getVirtualFileForArchive(f))
    }
    val props = containingJars.map(extractPropsFromJar)
    containingJars.zip(props).collect { case (file, Some(p)) => file -> p }.toSeq
  }

  private def extractPropsFromJar(jarFile: VirtualFile): Option[ExtensionProps] = {
    import com.google.gson._
    val propsVF = jarFile.findFileByRelativePath(s"META-INF/$PROPS_NAME")
    Option(propsVF)
      .flatMap(x =>
        Using.resource(new InputStreamReader(x.getInputStream)) { reader =>
          val props = new Gson().fromJson(reader, classOf[ExtensionProps])
          validateProps(props) match {
            case Left(error)   =>
              LOG.error(error + s"\nFrom file: $jarFile")
              None
            case Right(result) => Some(result)
          }
        }
      )
  }

  private def validateProps(props: ExtensionProps): Either[String, ExtensionProps] = {
    if ((props.artifact == null || props.artifact.isEmpty) && (props.urlOverride == null || props.urlOverride.isEmpty))
      return Left("Extension artifact and extension jar URL are not defined")
    if (props.artifact != null && props.artifact.count(_ == '%') < 2)
      return Left(s"Extension props is malformed: ${props.artifact}")
    Right(props)
  }

}
