package org.jetbrains.jps.incremental.scala.local

import org.jetbrains.jps.incremental.scala.shared.{Client, CompileEngine}
import org.jetbrains.plugins.scala.compiler.data.{CompilationData, CompilerData, ExpressionEvaluationArguments, SbtData}

import java.net.URLClassLoader
import java.nio.file.Path
import scala.jdk.CollectionConverters._

/**
 * Instantiated reflectively by [[CompileEngine.fromJpsDirectory]]
 */
final class CompileEngineImpl extends CompileEngine {

  private val server = new LocalServer

  private val classLoaderCache: Cache[Seq[Path], ClassLoader] = new Cache(3)

  override def compile(sbtData: SbtData, compilerData: CompilerData, compilationData: CompilationData, client: Client): Unit =
    server.doCompile(sbtData, compilerData, compilationData, client)

  // upstream: org.jetbrains.jps.incremental.scala.remote.Main.evaluateExpressionLogic
  override def evaluateExpression(arguments: ExpressionEvaluationArguments, client: Client): Unit = {
    val ExpressionEvaluationArguments(useBuiltInExpressionCompiler, outDir, classpath, scalacOptions, source, line, expression, localVariableNames, packageName) = arguments

    // upstream: the parent is the system class loader of the compile server, here the IDE classes must stay invisible
    val classLoader = classLoaderCache.getOrUpdate(classpath) { () =>
      new URLClassLoader(classpath.map(_.toUri.toURL).toArray, ClassLoader.getPlatformClassLoader)
    }

    val bridgeClassName =
      if (useBuiltInExpressionCompiler) "dotty.tools.debug.ExpressionCompilerBridge"
      else "dotty.tools.dotc.ExpressionCompilerBridge"

    val bridgeClass = Class.forName(bridgeClassName, true, classLoader)
    val bridgeInstance = bridgeClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    val runMethod = bridgeClass.getMethods.find(_.getName == "run").get

    val consumer: java.util.function.Consumer[String] = client.error(_)
    val expressionClassName = "CompiledExpression"
    val classpathString = classpath.mkString(java.io.File.pathSeparator)
    val scalacOptionsArray = scalacOptions.toArray
    val localVariableNamesJavaSet = localVariableNames.asJava

    if (useBuiltInExpressionCompiler) {
      val config = expressionCompilerConfig(classLoader, packageName, expressionClassName, line, expression, localVariableNamesJavaSet, consumer)
      runMethod.invoke(bridgeInstance, outDir, classpathString, scalacOptionsArray, source, config)
    } else {
      runMethod.invoke(bridgeInstance, outDir, expressionClassName, classpathString, scalacOptionsArray, source, Int.box(line),
        expression, localVariableNamesJavaSet, packageName, consumer, java.lang.Boolean.FALSE)
    }
  }

  private def expressionCompilerConfig(
    classLoader: ClassLoader,
    packageName: String,
    outputClassName: String,
    breakpointLine: Int,
    expression: String,
    localVariables: java.util.Set[String],
    errorReporter: java.util.function.Consumer[String]
  ): AnyRef = {
    val configClass = Class.forName("dotty.tools.debug.ExpressionCompilerConfig", true, classLoader)

    def invoke(config: AnyRef, name: String, parameterType: Class[?], argument: AnyRef): AnyRef =
      configClass.getDeclaredMethod(name, parameterType).invoke(config, argument)

    var config = configClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    config = invoke(config, "withPackageName", classOf[String], packageName)
    config = invoke(config, "withOutputClassName", classOf[String], outputClassName)
    config = invoke(config, "withBreakpointLine", classOf[Int], Int.box(breakpointLine))
    config = invoke(config, "withExpression", classOf[String], expression)
    config = invoke(config, "withLocalVariables", classOf[java.util.Set[String]], localVariables)
    config = invoke(config, "withErrorReporter", classOf[java.util.function.Consumer[String]], errorReporter)
    config
  }
}
