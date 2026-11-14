package org.jetbrains.jps.incremental.scala.local.worksheet

import org.jetbrains.jps.incremental.scala.local.worksheet.repl_interface.{ILoopWrapper, ILoopWrapperReporter, NoopReporter, PrintWriterReporter}
import org.jetbrains.jps.incremental.scala.local.worksheet.util.IOUtils
import org.jetbrains.jps.incremental.scala.CompileServerBundle
import org.jetbrains.jps.incremental.scala.shared.{Client, compilerVersion}
import org.jetbrains.plugins.scala.compiler.data.worksheet.WorksheetArgs
import org.jetbrains.plugins.scala.compiler.data.{CompilerJars, SbtData}
import org.jetbrains.plugins.scala.rt.project.Version

import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}

class ILoopWrapperFactoryHandler {
  import ILoopWrapperFactoryHandler._

  private var cachedReplFactory: Option[CachedReplFactory] = None

  def loadReplWrapperAndRun(
    args: WorksheetArgs.RunRepl,
    replContext: ReplContext,
    out: PrintStream, // the consumed output is delegated to client then encoded and delegated to Nailgun out and sent to the
    client: Client
  ): Unit = try {
    val compilerJars = replContext.compilerJars
    val compilerVersionFromProperties  = compilerVersion(compilerJars.compilerJar)
    val scalaVersion = compilerVersionFromProperties.fold(FallBackScalaVersion)(ScalaVersion.apply)
    val replWrapperClassName = wrapperClassNameFor(scalaVersion)

    if (args.dropCachedReplInstance) {
      cachedReplFactory.foreach(_.replFactory.clearSession(args.sessionId))
    }

    // TODO: improve caching, for now we can have only 1 instance with 1 version of scala
    val cachedFactory = cachedReplFactory match {
      case Some(cached@CachedReplFactory(_, _, oldVersion)) if oldVersion == scalaVersion =>
        client.internalDebug("using cached cachedReplFactory")
        cached
      case _ =>
        client.internalDebug("creating new cachedReplFactory")
        val loader = createClassLoader(compilerJars)
        val iLoopWrapper = new ILoopWrapperFactory
        cachedReplFactory.foreach(_.replFactory.clearCaches())
        val cached = CachedReplFactory(loader, iLoopWrapper, scalaVersion)
        cached
    }

    cachedReplFactory = Some(cachedFactory)

    client.progress(CompileServerBundle.message("running.repl"))
    IOUtils.patchSystemOut(out)
    val factory = cachedFactory.replFactory
    factory.loadReplWrapperAndRun(args, replContext, out, replWrapperClassName, scalaVersion, client, cachedFactory.classLoader)
  } catch {
    case e: InvocationTargetException =>
      throw e.getTargetException
  }
}

//noinspection TypeAnnotation
object ILoopWrapperFactoryHandler {

  private case class CachedReplFactory(
    classLoader: ClassLoader,
    replFactory: ILoopWrapperFactory,
    scalaVersion: ScalaVersion
  )

  // 2.12 works OK for 2.11 as well
  private final val ILoopWrapper212Impl    = "ILoopWrapper212Impl"
  private final val ILoopWrapper212_13Impl = "ILoopWrapper212_13Impl"
  private final val ILoopWrapper213_0Impl  = "ILoopWrapper213_0Impl"
  private final val ILoopWrapper213Impl    = "ILoopWrapper213Impl"
  private final val ILoopWrapper213_12Impl = "ILoopWrapper213_12Impl"
  private final val ILoopWrapper300Impl    = "ILoopWrapper300Impl"
  private final val ILoopWrapper312Impl    = "ILoopWrapper312Impl"
  private final val ILoopWrapper330Impl    = "ILoopWrapper330Impl"
  private final val ILoopWrapper38Impl     = "ILoopWrapper38Impl"

  private def wrapperClassNameFor(version: ScalaVersion): String = {
    val versionStr = version.value.presentation

    val wrapper = if (versionStr.startsWith("2.13.0")) ILoopWrapper213_0Impl
    else if (versionStr.startsWith("2.13")) {
      if (version.value >= Version("2.13.12")) ILoopWrapper213_12Impl
      else ILoopWrapper213Impl
    }
    else if (version.isScala3) {
      //TODO: this is basically equivalent to `3.0.0 <= version < 3.1.2,
      //  reuse org.jetbrains.plugins.scala.project.Version
      if (versionStr.startsWith("3.0") || versionStr.startsWith("3.1.0") || versionStr.startsWith("3.1.1"))
        ILoopWrapper300Impl
      else if (versionStr.startsWith("3.1.2") || versionStr.startsWith("3.1.3") || versionStr.startsWith("3.2"))
        ILoopWrapper312Impl
      else if (versionStr.startsWith("3.3") || versionStr.startsWith("3.4") || versionStr.startsWith("3.5") || versionStr.startsWith("3.6") || versionStr.startsWith("3.7"))
        ILoopWrapper330Impl
      else
        ILoopWrapper38Impl
    }
    // note: lexicographic comparison is used, but it should work fine
    else if (version.value >= Version("2.12.13")) ILoopWrapper212_13Impl
    else ILoopWrapper212Impl
    wrapper
  }

  private[worksheet] case class ScalaVersion(value: Version) {
    // temp solution while dotty is evolving very fast
    val isScala3: Boolean = value.presentation.startsWith("3.")
  }

  private[worksheet] object ScalaVersion {
    def apply(versionStr: String): ScalaVersion = ScalaVersion(Version(versionStr))
  }

  private val FallBackScalaVersion = ScalaVersion("2.12.0")

  // upstream: parsed the jar path from the class resource URL
  private def findContainingJar(clazz: Class[?]): Option[Path] =
    Option(clazz.getProtectionDomain.getCodeSource)
      .map(source => Path.of(source.getLocation.toURI))
      .filter(path => Files.isRegularFile(path))

  private def createClassLoader(compilerJars: CompilerJars): URLClassLoader = {
    val iLoopWrapperClass = classOf[ILoopWrapper]
    val wrapperImplsJar =
      findContainingJar(iLoopWrapperClass)
        .map(_.getParent.getParent.resolve("worksheet-repl-interface").resolve("impls.jar"))
        .toSeq

    val jars = wrapperImplsJar ++ compilerJars.allJars ++ compilerJars.replClasspath
    new ReplClassLoader(jars.map(_.toUri.toURL).toArray, classOf[ILoopWrapper].getClassLoader)
  }

  /**
   * upstream: the parent is the class loader of the REPL interface (the compile server class path), here it is the
   * class loader of the compile engine which sees the classes of the IDE (the Scala library of the plugin), so only
   * the REPL interface classes are taken from there, everything else - from the Scala compiler of the worksheet module
   */
  private final class ReplClassLoader(urls: Array[URL], replInterfaceLoader: ClassLoader)
    extends URLClassLoader("scala-worksheet-repl", urls, ClassLoader.getPlatformClassLoader) {

    override def loadClass(name: String, resolve: Boolean): Class[?] =
      if (ReplInterfaceClasses.exists(c => name == c.getName || name.startsWith(c.getName + "$")))
        replInterfaceLoader.loadClass(name)
      else
        super.loadClass(name, resolve)
  }

  private val ReplInterfaceClasses: Seq[Class[?]] =
    Seq(classOf[ILoopWrapper], classOf[ILoopWrapperReporter], classOf[NoopReporter], classOf[PrintWriterReporter])

  case class ReplContext(sbtData: SbtData,
                         compilerJars: CompilerJars,
                         classpath: Seq[Path],
                         scalacOptions: Seq[String])
}