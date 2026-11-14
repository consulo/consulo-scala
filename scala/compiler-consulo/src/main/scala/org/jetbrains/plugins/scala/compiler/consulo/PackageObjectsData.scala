package org.jetbrains.plugins.scala.compiler.consulo

import consulo.compiler.CompilerPaths
import consulo.logging.Logger
import consulo.project.Project

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/**
 * The sources of the base classes of package objects (IDEA incrementality): a change of a base class source must
 * recompile the package object.
 * upstream: org.jetbrains.jps.incremental.scala.local.PackageObjectsData (java serialization in the JPS data storage)
 */
private final class PackageObjectsData private(file: Path) {

  private val baseSourceToPackageObjects = mutable.HashMap.empty[Path, Set[Path]]

  def add(baseSource: Path, packageObject: Path): Unit = synchronized {
    baseSourceToPackageObjects.update(baseSource, baseSourceToPackageObjects.getOrElse(baseSource, Set.empty) + packageObject)
  }

  def invalidatedPackageObjects(sources: Set[Path]): Set[Path] = synchronized {
    sources.flatMap(baseSourceToPackageObjects.getOrElse(_, Set.empty)) -- sources
  }

  def clear(): Unit = synchronized {
    baseSourceToPackageObjects.clear()
  }

  def save(): Unit = synchronized {
    try {
      Files.createDirectories(file.getParent)
      val lines = for {
        (baseSource, packageObjects) <- baseSourceToPackageObjects.toSeq
        packageObject <- packageObjects
      } yield s"$baseSource\t$packageObject"
      Files.write(file, lines.asJava, StandardCharsets.UTF_8)
    } catch {
      case NonFatal(e) => PackageObjectsData.Log.warn("Could not save data about package objects dependencies", e)
    }
  }

  private def load(): Unit = synchronized {
    if (Files.exists(file)) {
      try {
        Files.readAllLines(file, StandardCharsets.UTF_8).asScala.foreach { line =>
          line.split('\t') match {
            case Array(baseSource, packageObject) => add(Path.of(baseSource), Path.of(packageObject))
            case _ =>
          }
        }
      } catch {
        case NonFatal(e) =>
          PackageObjectsData.Log.warn("Could not read data about package objects dependencies", e)
          clear()
          Files.deleteIfExists(file)
      }
    }
  }
}

private object PackageObjectsData {

  private val Log = Logger.getInstance(classOf[PackageObjectsData])

  private val instances = mutable.HashMap.empty[Path, PackageObjectsData]

  def getFor(project: Project): PackageObjectsData = {
    val file = CompilerPaths.getCompilerSystemDirectory(project).toPath.resolve("scala").resolve("packageObjects.dat")
    synchronized {
      instances.getOrElseUpdate(file, {
        val data = new PackageObjectsData(file)
        data.load()
        data
      })
    }
  }
}
