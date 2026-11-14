package org.jetbrains.plugins.scala.compiler.consulo

import consulo.compiler.{CompileContext, CompilerMessageCategory, TranslatingCompiler}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.util.io.FileUtil
import consulo.util.lang.ExceptionUtil
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.jps.incremental.scala.shared.remote.CompileServerMetrics
import org.jetbrains.jps.incremental.scala.shared.{Client, MessageKind}
import org.jetbrains.jps.incremental.scala.utils.ScalaJDKIncompatibilityDetector

import consulo.internal.org.objectweb.asm.ClassReader
import org.jetbrains.plugins.scala.rt.util.ScalaBytecodeConstants.PackageObjectSingletonClassName

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * upstream: org.jetbrains.jps.incremental.scala.local.IdeClient + IdeClientSbt / IdeClientIdea, reports to the Consulo
 * CompileContext and collects the generated class files as output items
 */
private final class ConsuloClient(context: CompileContext, target: BuildTarget) extends Client {

  private val Log = Logger.getInstance(classOf[ConsuloClient])

  private var hasErrors = false
  private val generatedFiles = mutable.ArrayBuffer.empty[(Path, Path)]
  private val generatedClassSources = mutable.HashMap.empty[String, Path]
  private val startedSources = mutable.LinkedHashSet.empty[Path]

  override def message(msg: Client.ClientMsg): Unit = {
    val Client.ClientMsg(kind, text, source, pointer, _, _, _) = msg

    val category = kind match {
      case MessageKind.Error | MessageKind.InternalBuilderError => CompilerMessageCategory.ERROR
      case MessageKind.Warning => CompilerMessageCategory.WARNING
      case MessageKind.Progress =>
        progress(text)
        return
      case MessageKind.Info | MessageKind.JpsInfo | MessageKind.Other => CompilerMessageCategory.INFORMATION
    }
    if (category == CompilerMessageCategory.ERROR) {
      hasErrors = true
    }

    val textWithOptionalJdkWarning =
      if (kind.isErrorOrWarning) ScalaJDKIncompatibilityDetector.prependWithWarning(text, Some(Runtime.version().feature()))
      else text

    val builder = context.newMessage(category, LocalizeValue.of(textWithOptionalJdkWarning))
    source.foreach(path => builder.url(VirtualFileUtil.pathToUrl(path.toPath.toAbsolutePath.normalize().toString)))
    pointer.foreach(position => builder.position(position.line, position.column))
    builder.add()
  }

  override def trace(exception: Throwable): Unit = {
    hasErrors = true
    Log.warn(exception)
    context.newError(LocalizeValue.of(ExceptionUtil.getThrowableText(exception))).add()
  }

  override def progress(text: String, done: Option[Float]): Unit = {
    val indicator = context.getProgressIndicator
    indicator.setText2(LocalizeValue.of(text))
    done.foreach(fraction => indicator.setFraction(fraction.toDouble))
  }

  override def internalInfo(text: String): Unit = Log.info(text)

  override def internalDebug(text: String): Unit = Log.debug(text)

  override def internalTrace(text: String): Unit = Log.trace(text)

  override def generated(source: Path, outputFile: Path, name: String): Unit = {
    val normalizedSource = source.toAbsolutePath.normalize()
    generatedFiles += normalizedSource -> outputFile.toAbsolutePath.normalize()
    if (name != null) {
      generatedClassSources.update(name, normalizedSource)
    }
  }

  // the class files deleted by zinc are not registered as outputs anymore once their sources are recompiled
  override def deleted(module: Path): Unit = ()

  override def isCanceled: Boolean = context.getProgressIndicator.isCanceled

  override def worksheetOutput(text: String): Unit = ()

  override def compilationStart(): Unit =
    context.getProgressIndicator.setText(LocalizeValue.localizeTODO(s"Compiling ${target.presentableName}"))

  override def compilationPhase(name: String): Unit = ()

  override def compilationUnit(path: String): Unit = ()

  override def compilationEnd(sources: Set[Path]): Unit = ()

  override def processingEnd(): Unit = ()

  // upstream (IdeClientSbt): FSOperations.markDirty(context, CompilationRound.NEXT, source)
  override def sourceStarted(source: String): Unit =
    startedSources += Path.of(source).toAbsolutePath.normalize()

  override def metrics(value: CompileServerMetrics): Unit = ()

  def hasReportedErrors: Boolean = hasErrors

  /**
   * (base class source, package object source) of the package objects compiled in this run whose base classes were
   * compiled in this run too. upstream: IdeClientIdea.handlePackageObject / persistPackageObjectData
   */
  def packageObjectBaseSources: Seq[(Path, Path)] =
    for {
      (packageObjectSource, classFile) <- generatedFiles.toSeq
      if classFile.getFileName.toString == s"$PackageObjectSingletonClassName.class"
      reader <- readClass(classFile).toSeq
      packageName = reader.getClassName.stripSuffix(PackageObjectSingletonClassName).replace('/', '.')
      baseType <- reader.getInterfaces.toSeq ++ Option(reader.getSuperName).filterNot(_ == "java/lang/Object")
      baseClassName = baseType.replace('/', '.')
      if baseClassName.startsWith(packageName)
      baseSource <- generatedClassSources.get(baseClassName).toSeq
    } yield baseSource -> packageObjectSource

  private def readClass(classFile: Path): Option[ClassReader] =
    try Some(new ClassReader(Files.readAllBytes(classFile)))
    catch {
      case NonFatal(e) =>
        Log.warn("Error parsing generated class " + classFile, e)
        None
    }

  /**
   * Registers the compilation results: the generated class files, the compiled sources without class files
   * and the sources that must be compiled again (upstream: OutputConsumer.registerCompiledClass and FSOperations.markDirty)
   */
  def registerOutputs(output: Path, sources: Seq[Path], sink: TranslatingCompiler.OutputSink): Boolean = {
    val toRecompile = if (hasErrors || isCanceled) startedSources ++ sources else startedSources
    val items = new java.util.ArrayList[TranslatingCompiler.OutputItem]()
    generatedFiles.foreach { case (source, classFile) =>
      if (!toRecompile.contains(source))
        items.add(new TranslatingCompiler.OutputItem(FileUtil.toSystemIndependentName(classFile.toString), source))
    }
    val withOutputs = generatedFiles.iterator.map(_._1).toSet
    sources.foreach { source =>
      if (!withOutputs.contains(source) && !toRecompile.contains(source))
        items.add(new TranslatingCompiler.OutputItem(null, source))
    }
    val recompile = new java.util.ArrayList[Path]()
    toRecompile.foreach(recompile.add)
    sink.add(FileUtil.toSystemIndependentName(output.toString), items, recompile)
    generatedFiles.nonEmpty
  }
}
