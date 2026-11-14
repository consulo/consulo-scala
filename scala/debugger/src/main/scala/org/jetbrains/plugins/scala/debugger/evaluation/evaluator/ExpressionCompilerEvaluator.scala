package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.engine.JVMNameUtil
import com.intellij.java.debugger.impl.engine.evaluation.expression.{Evaluator, IdentityEvaluator, UnBoxingEvaluator}
import com.intellij.java.debugger.engine.evaluation.expression.{ExpressionEvaluator, Modifier}
import com.intellij.java.debugger.engine.evaluation.EvaluationContext
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl
import com.intellij.java.debugger.impl.DebuggerUtilsEx
import consulo.annotation.access.{RequiredReadAction => RequiresReadLock}
import consulo.language.util.ModuleUtilCore
import consulo.module.Module
import consulo.module.content.layer.OrderEnumerator
import consulo.language.psi.PsiElement
import consulo.virtualFileSystem.VirtualFile
import consulo.internal.com.sun.jdi.{ArrayReference, ArrayType, ClassLoaderReference, ClassType, ObjectReference, Value}
import org.jetbrains.plugins.scala.compiler.data.ExpressionEvaluationArguments
import consulo.compiler.CompilerPaths
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator.ExpressionCompilerEvaluator.filteredScalacOptions
import org.jetbrains.plugins.scala.debugger.evaluation.{EvaluationException, EvaluatorCompiler, ExpressionCompilerResolverListener}
import org.jetbrains.plugins.scala.debugger.{DebuggerBundle, ScalaPositionManager}
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

private[evaluation] final class ExpressionCompilerEvaluator @RequiresReadLock (codeFragment: PsiElement, position: SourcePosition) extends ExpressionEvaluator {

  // Everything derived from PSI is computed here, under the read action the evaluator is built in; evaluate() only
  // talks to the compiler and the debuggee
  private val moduleForPsiElement: Module = ModuleUtilCore.findModuleForPsiElement(codeFragment)
  private val codeFragmentText: String = codeFragment.getText
  private val sourceFile: VirtualFile = position.getFile.getVirtualFile
  private val line: Int = position.getLine + 1
  private val packageName: String = ScalaPositionManager.findPackageName(position.getElementAt).getOrElse("")

  override def getModifier: Modifier = null

  override def evaluate(context: EvaluationContext): Value = {
    if (moduleForPsiElement eq null) {
      throw EvaluationException(DebuggerBundle.message("could.not.find.module.for.code.fragment", codeFragmentText))
    }

    val module = moduleForPsiElement.findRepresentativeModuleForSharedSourceModuleOrSelf

    val outDir = createOutputDirectory(CompilerPaths.getCompilerSystemDirectory(context.getProject).toPath)

    val scalaVersion = module.scalaMinorVersion match {
      case Some(version) => version
      case None =>
        throw EvaluationException(DebuggerBundle.message("could.not.determine.scala.version", module.getName))
    }

    val expressionCompilerType = {
      import ExpressionCompilerResolverListener.{ExpressionCompilers, ScalaExpressionCompilerVersion}
      context.getProject.getUserData(ExpressionCompilers)
        .getOrElse(scalaVersion, throw EvaluationException(DebuggerBundle.message("could.not.resolve.scala.expression.compiler", ScalaExpressionCompilerVersion, scalaVersion.minor)))
    }

    import ExpressionCompilerResolverListener.ExpressionCompilerType
    val expressionCompilerJar = expressionCompilerType match {
      case ExpressionCompilerType.BuiltIn => Seq.empty
      case ExpressionCompilerType.ResolvedJar(path) => Seq(path)
    }
    val useBuiltInExpressionCompiler = expressionCompilerType == ExpressionCompilerType.BuiltIn

    try {
      def stripJarPathSuffix(path: String): String =
        path.stripSuffix("!").stripSuffix("!/")

      val enumerator = OrderEnumerator.orderEntries(module).compileOnly().recursively()
      val classpath =
        (module.scalaCompilerClasspath ++
          enumerator.getClassesRoots.map(_.getCanonicalPath).map(stripJarPathSuffix).map(Path.of(_)) ++
          expressionCompilerJar).distinct
      val scalacOptions = filteredScalacOptions(ScalaCompilerSettings.forModule(module).getOptionsAsStrings(module.hasScala3))
      val source = Path.of(sourceFile.getCanonicalPath)

      val stackFrame = context.getFrameProxy.getStackFrame
      val localVariables = stackFrame.visibleVariables().asScala
      val (localVariableNames, localVariableValues) = {
        val names = localVariables.map(_.name())
        val values = localVariables.map(stackFrame.getValue)
        names -> values
      }
      val thisObject = stackFrame.thisObject()

      val arguments = ExpressionEvaluationArguments(useBuiltInExpressionCompiler, outDir, classpath, scalacOptions, source, line, codeFragmentText, localVariableNames.toSet, packageName)

      // upstream: evaluated by the Scala compile server
      EvaluatorCompiler.evaluateExpression(arguments) match {
        case Left(errors) =>
          val message = DebuggerBundle.message("expression.compilation.failed", errors.mkString(System.lineSeparator()))
          throw EvaluationException(message)
        case Right(()) =>
      }

      val autoLoadContext = context.asInstanceOf[EvaluationContextImpl].withAutoLoadClasses(true)
      val classLoader = createClassLoader(outDir, autoLoadContext)
      autoLoadContext.setClassLoader(classLoader)
      val prefix = if (packageName == "") "" else s"$packageName."
      val className = s"${prefix}CompiledExpression"
      autoLoadContext.getDebugProcess.findClass(autoLoadContext, className, classLoader)

      val localVariableNamesEvaluator: Evaluator = new Evaluator {
        override def evaluate(ctx: EvaluationContextImpl): AnyRef = {
          val arrayType = ctx.getDebugProcess.findClass(ctx, "java.lang.String[]", ctx.getClassLoader).asInstanceOf[ArrayType]
          val array = mirrorOfArray(arrayType, localVariableNames.length, ctx)
          array.setValues(localVariableNames.map(DebuggerUtilsEx.mirrorOfString(_, ctx)).asJava)
          array
        }
      }

      val localVariableValuesEvaluator: Evaluator = new Evaluator {
        override def evaluate(ctx: EvaluationContextImpl): AnyRef = {
          val arrayType = ctx.getDebugProcess.findClass(ctx, "java.lang.Object[]", ctx.getClassLoader).asInstanceOf[ArrayType]
          val array = mirrorOfArray(arrayType, localVariableValues.length, ctx)
          array.setValues(localVariableValues.map(ScalaBoxingEvaluator.box(_, ctx).asInstanceOf[Value]).asJava)
          array
        }
      }

      val thisEvaluator = new IdentityEvaluator(thisObject)
      val instance = ScalaMethodEvaluator(new ScalaTypeEvaluator(JVMNameUtil.getJVMRawText(className)), "<init>", null, Seq(thisEvaluator, localVariableNamesEvaluator, localVariableValuesEvaluator))
      val method = ScalaMethodEvaluator(instance, "evaluate", null, Seq.empty)
      val unboxed = new UnBoxingEvaluator(method)

      unboxed.evaluate(autoLoadContext).asInstanceOf[Value]
    } catch {
      case NonFatal(t) => throw EvaluationException(t)
    } finally {
      Files.walkFileTree(outDir, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          if (exc ne null) {
            throw exc
          }
          if (outDir != dir) {
            Files.delete(dir)
          }
          FileVisitResult.CONTINUE
        }
      })
    }
  }

  private def createOutputDirectory(workingDir: Path): Path = {
    val path = workingDir.resolve("scala-debugger").resolve("out")
    if (!path.exists) {
      Files.createDirectories(path)
    }
    path
  }

  private def createClassLoader(outDir: Path, context: EvaluationContextImpl): ClassLoaderReference = {
    val process = context.getDebugProcess
    val thread = context.getFrameProxy.getStackFrame.thread()
    val classLoader = context.getClassLoader
    val uriString = directoryUriString(outDir)
    val pathURL = DebuggerUtilsEx.mirrorOfString(uriString, context)
    val urlType = process.findClass(context, "java.net.URL", classLoader).asInstanceOf[ClassType]
    val urlConstructor = urlType.concreteMethodByName("<init>", "(Ljava/lang/String;)V")
    val url = urlType.newInstance(thread, urlConstructor, List(pathURL).asJava, ObjectReference.INVOKE_SINGLE_THREADED)
    val urlArrayType = process.findClass(context, "java.net.URL[]", classLoader).asInstanceOf[ArrayType]
    val array = mirrorOfArray(urlArrayType, 1, context)
    array.setValue(0, url)
    val urlClassLoaderType = process.findClass(context, "java.net.URLClassLoader", classLoader).asInstanceOf[ClassType]
    val urlClassLoaderConstructor = urlClassLoaderType.concreteMethodByName("<init>", "([Ljava/net/URL;Ljava/lang/ClassLoader;)V")
    urlClassLoaderType.newInstance(thread, urlClassLoaderConstructor, List(array, classLoader).asJava, ObjectReference.INVOKE_SINGLE_THREADED).asInstanceOf[ClassLoaderReference]
  }

  // upstream: the URI of the directory on the eel of the debuggee
  private def directoryUriString(directory: Path): String =
    directory.toUri.toURL.toString + "/"

  // upstream: DebuggerUtilsEx.mirrorOfArray
  private def mirrorOfArray(arrayType: ArrayType, dimension: Int, context: EvaluationContextImpl): ArrayReference = {
    val array = context.getDebugProcess.newInstance(arrayType, dimension)
    DebuggerUtilsEx.keep(array, context)
    array
  }
}

private object ExpressionCompilerEvaluator {
  private final val IgnoredScalacOptions: Array[String] = Array("-Werror", "-Xfatal-warnings")

  private def filteredScalacOptions(scalacOptions: Seq[String]): Seq[String] =
    scalacOptions.filterNot(IgnoredScalacOptions.contains)
}
