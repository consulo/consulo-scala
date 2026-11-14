package org.jetbrains.plugins.scala.decompileToJava

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton

import consulo.language.codeStyle.CodeStyle
import com.intellij.java.language.impl.{JavaClassFileType, JavaFileType}
import consulo.util.io.FileUtil
import consulo.virtualFileSystem.VirtualFile
import consulo.logging.Logger
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler
import org.jetbrains.java.decompiler.main.extern.{IBytecodeProvider, IFernflowerLogger, IFernflowerPreferences, IResultSaver}
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

import java.util.jar.Manifest
import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}

@Singleton
@ServiceImpl
private class ScalaDecompilerServiceImpl extends ScalaDecompilerService {

  import ScalaDecompilerServiceImpl._

  override def decompile(file: ScFile): Try[String] = if (file.isCompiled) {
    try {
      val mappings = inReadAction {
        mappingsForClassfile(file.getVirtualFile)
      }
      val saver = new ScalaResultSaver

      val provider: IBytecodeProvider = (externalPath, _) => {
        val path = FileUtil.toSystemIndependentName(externalPath)
        mappings.get(path).map(_.apply()).orNull
      }
      val options: Map[String, AnyRef] = getFernflowerDecompilerOptions
      val decompiler = new BaseDecompiler(provider, saver, options.asJava, new ScalaFernflowerLogger)
      mappings.foreach { case (path, _) => decompiler.addSpace(new java.io.File(path), true) }
      decompiler.decompileContext()
      Success(saver.result)
    } catch {
      case e: InternalException =>
        Failure(e)
    }
  }
  else Failure(new RuntimeException(s"Unable to decompile ${file.getName}"))

  private def isClassGeneratedFrom(sourceName: String, classfile: VirtualFile): Boolean =
    classfile.getFileType == JavaClassFileType.INSTANCE && {
      val name = classfile.getNameWithoutExtension
      name == sourceName || name.startsWith(sourceName + "$")
    }

  /**
   * Using String as a key because the path can reference to a .class file inside a jar.
   * This is not a valid nio path but rather an URI
   * Example:<br>
   * jar:///Users/dmitrii.naumenko/.sbt/boot/scala-2.12.20/org.scala-sbt/sbt/1.10.7/main_2.12-1.10.7.jar!/sbt/nio
   */
  private def mappingsForClassfile(file: VirtualFile): Map[String, FileContents] =
    file.getParent.getChildren.iterator.collect {
      case child if isClassGeneratedFrom(file.getNameWithoutExtension, child) =>
        child.getPath -> getFileContents(child)
    }.toMap
}

object ScalaDecompilerServiceImpl {
  type FileContents = () => Array[Byte]

  /**
   *  Original text inspired by [[org.jetbrains.java.decompiler.IdeaDecompilerKt.IDEA_DECOMPILER_BANNER]].<br>
   *  We use custom text because the result of compilation is concatenation of multiple compilation results, not a single `.class` file
   *  Original wording was "recreated from a .class file by IntelliJ IDEA"
   */
  private val Banner =
    """//
      |// Source code recreated by IntelliJ IDEA
      |// (powered by FernFlower decompiler)
      |//
      |""".stripMargin.replace("\r", "")

  /**
   * @note for options used for Java .class files decompiler see:<br>
   *       [[org.jetbrains.java.decompiler.IdeaDecompilerKt.getOptions]]<br>
   *       Some of the option values were taken from there
   */
  private def getFernflowerDecompilerOptions: Map[String, AnyRef] = {
    val options = CodeStyle.getDefaultSettings.getIndentOptions(JavaFileType.INSTANCE)
    val indent = " " * options.INDENT_SIZE

    val DISABLED = "0"
    val ENABLED = "1"

    Map(
      IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR -> DISABLED,
      IFernflowerPreferences.INDENT_STRING -> indent,

      //I didn't find the original reasoning why it's disabled, but I guess in Scala it might be useful to see more internals
      IFernflowerPreferences.REMOVE_BRIDGE -> DISABLED,
      //I guess the same reasoning as with `REMOVE_BRIDGE`
      IFernflowerPreferences.REMOVE_SYNTHETIC -> DISABLED,

      //This option is needed in order it works on Windows (otherwise the result will contain `\r` and document.setText won't work)
      IFernflowerPreferences.NEW_LINE_SEPARATOR -> ENABLED,

      //example: show `Option<String> foo()` instead of `Option foo()`
      IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES -> ENABLED,

      //avoid very long decompilation
      //value is copied from IdeaDecompilerKt
      IFernflowerPreferences.MAX_PROCESSING_METHOD -> 60.asInstanceOf[java.lang.Integer],

      // upstream: IGNORE_INVALID_BYTECODE -> ENABLED (not supported by the bundled fernflower)

      //Skipping the BANNER - we will append the banner manually after concatenating all decompilation results for a given file
      //IFernflowerPreferences.BANNER -> ScalaDecompilerServiceImpl.BANNER,

      //it's used in IdeaDecompilerKt, but I have no idea what it affects from the user perspective
      //IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES -> ENABLED,
    )
  }

  private val Log = Logger.getInstance(classOf[ScalaDecompilerServiceImpl])

  private class InternalException(message: String, cause: Throwable) extends RuntimeException(message, cause)

  // upstream: org.jetbrains.java.decompiler.IdeaLogger (not exported by the Consulo fernflower plugin)
  private class ScalaFernflowerLogger extends IFernflowerLogger {
    override def writeMessage(message: String, severity: IFernflowerLogger.Severity): Unit =
      writeMessage(message, severity, null)

    override def writeMessage(message: String, severity: IFernflowerLogger.Severity, t: Throwable): Unit = severity match {
      case IFernflowerLogger.Severity.TRACE => Log.debug(message, t)
      case IFernflowerLogger.Severity.INFO  => Log.info(message, t)
      case IFernflowerLogger.Severity.WARN  => Log.warn(message, t)
      case IFernflowerLogger.Severity.ERROR => Log.error(message, t)
    }

    override def writeMessage(message: String, t: Throwable): Unit = t match {
      case e: InternalException => throw e
      case _ => throw new InternalException(message, t)
    }
  }

  private def getFileContents(vf: VirtualFile): FileContents =
    () => vf.contentsToByteArray(false)

  private class ScalaResultSaver extends IResultSaver {
    private val fileNamesWithDecompiledText: mutable.Buffer[(String, String)] =
      mutable.ArrayBuffer.empty

    def result: String = {
      val concatenatedResults = fileNamesWithDecompiledText
        .map { case (fileName, decompiledText) =>
          s"//decompiled from ${fileName.stripSuffix(".java")}.class\n" +
            decompiledText
        }
        .mkString("\n")
      Banner + "\n" + concatenatedResults
    }

    override def saveClassFile(path: String, qname: String, entry: String, content: String, mapping: Array[Int]): Unit =
      if (entry != null && content != null) {
        fileNamesWithDecompiledText += entry -> content
      }

    override def saveClassEntry(path: String, archive: String, qname: String, entry: String, content: String): Unit = ()
    override def saveDirEntry(path: String, archive: String, entry: String): Unit = ()
    override def closeArchive(path: String, name: String): Unit = ()
    override def saveFolder(path: String): Unit = ()
    override def copyFile(source: String, path: String, entry: String): Unit = ()

    override def createArchive(path: String, name: String, manifest: Manifest): Unit = ()
    override def copyEntry(source: String, path: String, name: String, entry: String): Unit = ()
  }
}
