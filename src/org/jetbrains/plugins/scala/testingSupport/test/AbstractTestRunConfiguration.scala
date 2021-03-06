package org.jetbrains.plugins.scala
package testingSupport.test

import com.intellij.psi.search.GlobalSearchScope
import org.jdom.Element
import config.ScalaFacet
import collection.JavaConversions._
import com.intellij.openapi.options.{SettingsEditorGroup, SettingsEditor}
import com.intellij.diagnostic.logging.LogConfigurationPanel
import scala.beans.BeanProperty
import com.intellij.openapi.module.Module

import com.intellij.openapi.components.PathMacroManager
import lang.psi.impl.ScalaPsiManager
import lang.psi.api.toplevel.typedef.{ScClass, ScObject}
import com.intellij.psi.{PsiModifierList, PsiJavaPackage, JavaPsiFacade, PsiClass}
import com.intellij.openapi.util.{Computable, JDOMExternalizer, Getter}

import testingSupport.test.TestRunConfigurationForm.{SearchForTest, TestKind}
import com.intellij.execution._
import com.intellij.execution.runners.{ProgramRunner, ExecutionEnvironment}
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.openapi.project.Project
import com.intellij.util.PathUtil
import com.intellij.execution.configurations._
import java.lang.String
import lang.psi.ScalaPsiUtil
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.projectRoots.{JdkUtil, Sdk}
import testingSupport.ScalaTestingConfiguration
import testframework.sm.runner.ui.SMTRunnerConsoleView
import testframework.TestFrameworkRunningModel
import lang.psi.impl.ScPackageImpl
import extensions.toPsiClassExt
import lang.psi.api.ScPackage
import collection.mutable.ArrayBuffer
import testingSupport.test.AbstractTestRunConfiguration.PropertiesExtension
import org.jetbrains.plugins.scala.compiler.rt.ClassRunner
import lang.psi.api.toplevel.ScModifierListOwner
import com.intellij.openapi.application.ApplicationManager
import java.io.{IOException, FileOutputStream, PrintStream, File}
import scala.collection.mutable

/**
 * @author Ksenia.Sautina
 * @since 5/15/12
 */

abstract class AbstractTestRunConfiguration(val project: Project,
                                            val configurationFactory: ConfigurationFactory,
                                            val name: String)
  extends ModuleBasedConfiguration[RunConfigurationModule](name,
    new RunConfigurationModule(project),
    configurationFactory)
  with ScalaTestingConfiguration {

  val SCALA_HOME = "-Dscala.home="
  val CLASSPATH = "-Denv.classpath=\"%CLASSPATH%\""
  val EMACS = "-Denv.emacs=\"%EMACS%\""

  def currentConfiguration = AbstractTestRunConfiguration.this

  def suitePath: String

  def mainClass: String

  def reporterClass: String

  def errorMessage: String

  private var testClassPath = ""
  private var testPackagePath = ""
  private var testArgs = ""
  private var javaOptions = ""
  private var workingDirectory = {
    getModule.getModuleDirPath;
  }

  def getTestClassPath = testClassPath

  def getTestPackagePath = testPackagePath

  def getTestArgs = testArgs

  def getJavaOptions = javaOptions

  def getWorkingDirectory: String = ExternalizablePath.localPathValue(workingDirectory)

  def setTestClassPath(s: String) {
    testClassPath = s
  }

  def setTestPackagePath(s: String) {
    testPackagePath = s
  }

  def setTestArgs(s: String) {
    testArgs = s
  }

  def setJavaOptions(s: String) {
    javaOptions = s
  }

  def setWorkingDirectory(s: String) {
    workingDirectory = ExternalizablePath.urlValue(s)
  }

  @BeanProperty
  var searchTest: SearchForTest = SearchForTest.ACCROSS_MODULE_DEPENDENCIES
  @BeanProperty
  var testName = ""
  @BeanProperty
  var testKind = TestKind.CLASS
  @BeanProperty
  var showProgressMessages = true

  private var generatedName: String = ""

  override def getGeneratedName = generatedName

  def setGeneratedName(name: String) {
    generatedName = name
  }

  override def isGeneratedName = getName == null || getName.equals(suggestedName)

  override def suggestedName = getGeneratedName

  def apply(configuration: TestRunConfigurationForm) {
    testKind = configuration.getSelectedKind
    setTestClassPath(configuration.getTestClassPath)
    setTestPackagePath(configuration.getTestPackagePath)
    setSearchTest(configuration.getSearchForTest)
    setJavaOptions(configuration.getJavaOptions)
    setTestArgs(configuration.getTestArgs)
    setModule(configuration.getModule)
    setWorkingDirectory(configuration.getWorkingDirectory)
    setTestName(configuration.getTestName)
    setShowProgressMessages(configuration.getShowProgressMessages)
  }

  def getClazz(path: String, withDependencies: Boolean): PsiClass = {
    val classes = ScalaPsiManager.instance(project).getCachedClasses(getScope(withDependencies), path)
    val objectClasses = classes.filter(_.isInstanceOf[ScObject])
    val nonObjectClasses = classes.filter(!_.isInstanceOf[ScObject])
    if (nonObjectClasses.nonEmpty) nonObjectClasses(0) else if (objectClasses.nonEmpty) objectClasses(0) else null
  }

  def getPackage(path: String): PsiJavaPackage = {
    ScPackageImpl.findPackage(project, path)
  }

  def getScope(withDependencies: Boolean): GlobalSearchScope = {
    def mScope(module: Module) = {
      if (withDependencies) GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
      else GlobalSearchScope.moduleScope(module)
    }
    def unionScope(moduleGuard: Module => Boolean): GlobalSearchScope = {
      var scope: GlobalSearchScope = if (getModule != null) mScope(getModule) else GlobalSearchScope.EMPTY_SCOPE
      for (module <- ModuleManager.getInstance(getProject).getModules) {
        if (moduleGuard(module)) {
          scope = scope.union(mScope(module))
        }
      }
      scope
    }
    testKind match {
      case TestKind.ALL_IN_PACKAGE =>
        searchTest match {
          case SearchForTest.IN_WHOLE_PROJECT => unionScope(_ => true)
          case SearchForTest.IN_SINGLE_MODULE if getModule != null => mScope(getModule)
          case SearchForTest.ACCROSS_MODULE_DEPENDENCIES if getModule != null =>
            unionScope(ModuleManager.getInstance(getProject).isModuleDependent(getModule, _))
          case _ => unionScope(_ => true)
        }
      case _ =>
        if (getModule != null) mScope(getModule)
        else unionScope(_ => true)
    }
  }

  def expandPath(_path: String): String = {
    var path = _path
    path = PathMacroManager.getInstance(project).expandPath(path)
    if (getModule != null) {
      path = PathMacroManager.getInstance(getModule).expandPath(path)
    }
    path
  }

  def getModule: Module = {
    getConfigurationModule.getModule
  }

  def getValidModules: java.util.List[Module] = ScalaFacet.findModulesIn(getProject).toList

  override def getModules: Array[Module] = {
    ApplicationManager.getApplication.runReadAction(new Computable[Array[Module]] {
      @SuppressWarnings(Array("ConstantConditions"))
      def compute: Array[Module] = {
        searchTest match {
          case SearchForTest.ACCROSS_MODULE_DEPENDENCIES if getModule != null =>
            val buffer = new ArrayBuffer[Module]()
            buffer += getModule
            for (module <- ModuleManager.getInstance(getProject).getModules) {
              if (ModuleManager.getInstance(getProject).isModuleDependent(getModule, module)) {
                buffer += module
              }
            }
            buffer.toArray
          case SearchForTest.IN_SINGLE_MODULE if getModule != null => Array(getModule)
          case SearchForTest.IN_WHOLE_PROJECT => ModuleManager.getInstance(getProject).getModules
          case _ => Array.empty
        }
      }
    })
  }

  override def checkConfiguration() {
    super.checkConfiguration()

    val suiteClass = getClazz(suitePath, withDependencies = true)

    if (suiteClass == null) {
      throw new RuntimeConfigurationException(errorMessage)
    }

    testKind match {
      case TestKind.ALL_IN_PACKAGE =>
        searchTest match {
          case SearchForTest.IN_WHOLE_PROJECT =>
          case SearchForTest.IN_SINGLE_MODULE | SearchForTest.ACCROSS_MODULE_DEPENDENCIES =>
            if (getModule == null) {
              throw new RuntimeConfigurationException("Module is not specified")
            }
        }
        val pack = JavaPsiFacade.getInstance(project).findPackage(getTestPackagePath)
        if (pack == null) {
          throw new RuntimeConfigurationException("Package doesn't exist")
        }
      case TestKind.CLASS | TestKind.TEST_NAME =>
        if (getModule == null) {
          throw new RuntimeConfigurationException("Module is not specified")
        }
        if (getTestClassPath == "") {
          throw new RuntimeConfigurationException("Test Class is not specified")
        }
        val clazz = getClazz(getTestClassPath, withDependencies = false)
        if (clazz == null || isInvalidSuite(clazz)) {
          throw new RuntimeConfigurationException("No Suite Class is found for Class %s in module %s".format(getTestClassPath,
            getModule.getName))
        }
        if (!ScalaPsiUtil.cachedDeepIsInheritor(clazz, suiteClass)) {
          throw new RuntimeConfigurationException("Class %s is not inheritor of Suite trait".format(getTestClassPath))
        }
        if (testKind == TestKind.TEST_NAME && getTestName == "") {
          throw new RuntimeConfigurationException("Test Name is not specified")
        }
    }

    JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)
  }

  def getConfigurationEditor: SettingsEditor[_ <: RunConfiguration] = {
    val group: SettingsEditorGroup[AbstractTestRunConfiguration] = new SettingsEditorGroup
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
      new AbstractTestRunConfigurationEditor(project, this))
    JavaRunConfigurationExtensionManager.getInstance.appendEditors(this, group)
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel)
    group
  }

  protected[test] def isInvalidSuite(clazz: PsiClass): Boolean = AbstractTestRunConfiguration.isInvalidSuite(clazz)

  override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState = {
    def classNotFoundError() {
      throw new ExecutionException("Test class not found.")
    }
    var clazz: PsiClass = null
    var suiteClass: PsiClass = null
    var pack: ScPackage = null
    try {
      testKind match {
        case TestKind.CLASS =>
          clazz = getClazz(getTestClassPath, withDependencies = false)
        case TestKind.ALL_IN_PACKAGE =>
          pack = ScPackageImpl(getPackage(getTestPackagePath))
        case TestKind.TEST_NAME =>
          clazz = getClazz(getTestClassPath, withDependencies = false)
          if (getTestName == null || getTestName == "") throw new ExecutionException("Test name not found.")
      }
      suiteClass = getClazz(suitePath, withDependencies = true)
    }
    catch {
      case e if clazz == null => classNotFoundError()
    }
    if (clazz == null && pack == null) classNotFoundError()
    if (suiteClass == null)
      throw new ExecutionException(errorMessage)
    val classes = new mutable.HashSet[PsiClass]
    if (clazz != null) {
      if (ScalaPsiUtil.cachedDeepIsInheritor(clazz, suiteClass)) classes += clazz
    } else {
      val scope = getScope(withDependencies = false)
      def getClasses(pack: ScPackage): Seq[PsiClass] = {
        val buffer = new ArrayBuffer[PsiClass]

        buffer ++= pack.getClasses(scope)
        for (p <- pack.getSubPackages) {
          buffer ++= getClasses(ScPackageImpl(p))
        }
        buffer.toSeq
      }
      for (cl <- getClasses(pack)) {
        if (!isInvalidSuite(cl) && ScalaPsiUtil.cachedDeepIsInheritor(cl, suiteClass))
          classes += cl
      }
    }

    if (classes.isEmpty) throw new ExecutionException("Not found suite class.")

    val module = getModule
    if (module == null) throw new ExecutionException("Module is not specified")

    val state = new JavaCommandLineState(env) with AbstractTestRunConfiguration.TestCommandLinePatcher {
      val getClasses: Seq[String] = classes.map(_.qualifiedName).toSeq

      protected override def createJavaParameters: JavaParameters = {
        val params = new JavaParameters()

        params.setCharset(null)
        params.getVMParametersList.addParametersString(getJavaOptions)
        val wDir = getWorkingDirectory
        params.setWorkingDirectory(expandPath(wDir))

//        params.getVMParametersList.addParametersString("-Xnoagent -Djava.compiler=NONE -Xdebug " +
//          "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5010")

        val rtJarPath = PathUtil.getJarPathForClass(classOf[ClassRunner])
        params.getClassPath.add(rtJarPath)

        searchTest match {
          case SearchForTest.IN_WHOLE_PROJECT =>
            var jdk: Sdk = null
            for (module <- ModuleManager.getInstance(project).getModules if jdk == null) {
              jdk = JavaParameters.getModuleJdk(module)
            }
            params.configureByProject(project, JavaParameters.JDK_AND_CLASSES_AND_TESTS, jdk)
          case _ =>
            params.configureByModule(module, JavaParameters.JDK_AND_CLASSES_AND_TESTS, JavaParameters.getModuleJdk(module))
        }

        params.setMainClass(mainClass)

        if (JdkUtil.useDynamicClasspath(getProject)) {
          try {
            val fileWithParams: File = File.createTempFile("abstracttest", ".tmp")
            val outputStream = new FileOutputStream(fileWithParams)
            val printer: PrintStream = new PrintStream(outputStream)
            if (getFailedTests == null) {
              printer.println("-s")
              for (cl <- getClasses) {
                printer.println(cl)
              }
              if (testKind == TestKind.TEST_NAME && testName != "") {
                printer.println("-testName")
                printer.println(testName)
                params.getVMParametersList.addParametersString("-Dspecs2.ex=\"" + testName + "\"")
              }
            } else {
              printer.println("-failedTests")
              for (failed <- getFailedTests) {
                printer.println(failed._1)
                printer.println(failed._2)
                params.getVMParametersList.addParametersString("-Dspecs2.ex=\"" + failed._2 + "\"")
              }
            }

            val parms: Array[String] = ParametersList.parse(getTestArgs)
            for (parm <- parms) {
              printer.println(parm)
            }
            printer.println("-showProgressMessages")
            printer.println(showProgressMessages.toString)
            printer.println("-C")
            printer.println(reporterClass)

            printer.close()
            params.getProgramParametersList.add("@" + fileWithParams.getPath)
          }
          catch {
            case ioException: IOException => throw new ExecutionException("Failed to create dynamic classpath file with command-line args.", ioException)
          }
        } else {
          if (getFailedTests == null) {
            params.getProgramParametersList.add("-s")
            for (cl <- getClasses) params.getProgramParametersList.add(cl)
            if (testKind == TestKind.TEST_NAME && testName != "") {
              params.getProgramParametersList.add("-testName")
              params.getProgramParametersList.add(testName)
              params.getVMParametersList.addParametersString("-Dspecs2.ex=\"" + testName + "\"")
            }
          } else {
            params.getProgramParametersList.add("-failedTests")
            for (failed <- getFailedTests) {
              params.getProgramParametersList.add(failed._1)
              params.getProgramParametersList.add(failed._2)
              params.getVMParametersList.addParametersString("-Dspecs2.ex=\"" + failed._2 + "\"")
            }
          }

          params.getProgramParametersList.addParametersString(getTestArgs)
          params.getProgramParametersList.add("-showProgressMessages")
          params.getProgramParametersList.add(showProgressMessages.toString)

          params.getProgramParametersList.add("-C")
          params.getProgramParametersList.add(reporterClass)
        }

        for (ext <- Extensions.getExtensions(RunConfigurationExtension.EP_NAME)) {
          ext.updateJavaParameters(currentConfiguration, params, getRunnerSettings)
        }

        params
      }

      override def execute(executor: Executor, runner: ProgramRunner[_ <: RunnerSettings]): ExecutionResult = {
        val processHandler = startProcess
        val runnerSettings = getRunnerSettings
        if (getConfiguration == null) setConfiguration(currentConfiguration)
        val config = getConfiguration
        JavaRunConfigurationExtensionManager.getInstance.
          attachExtensionsToProcess(currentConfiguration, processHandler, runnerSettings)
        val consoleProperties = new SMTRunnerConsoleProperties(currentConfiguration, "Scala", executor)
          with PropertiesExtension {
          def getRunConfigurationBase: RunConfigurationBase = config
        }

        // console view
        val consoleView: BaseTestsOutputConsoleView = SMTestRunnerConnectionUtil.createAndAttachConsole("Scala",
          processHandler, consoleProperties, getEnvironment)

        val res = new DefaultExecutionResult(consoleView, processHandler,
          createActions(consoleView, processHandler, executor): _*)

        val rerunFailedTestsAction = new AbstractTestRerunFailedTestsAction(consoleView.getComponent)
        rerunFailedTestsAction.init(consoleView.getProperties, getEnvironment)
        rerunFailedTestsAction.setModelProvider(new Getter[TestFrameworkRunningModel] {
          def get: TestFrameworkRunningModel = {
            consoleView.asInstanceOf[SMTRunnerConsoleView].getResultsViewer
          }
        })
        res.setRestartActions(rerunFailedTestsAction)
        res
      }
    }
    state
  }

  override def writeExternal(element: Element) {
    super.writeExternal(element)
    JavaRunConfigurationExtensionManager.getInstance.writeExternal(this, element)
    writeModule(element)
    JDOMExternalizer.write(element, "path", getTestClassPath)
    JDOMExternalizer.write(element, "package", getTestPackagePath)
    JDOMExternalizer.write(element, "vmparams", getJavaOptions)
    JDOMExternalizer.write(element, "params", getTestArgs)
    JDOMExternalizer.write(element, "workingDirectory", workingDirectory)
    JDOMExternalizer.write(element, "searchForTest", searchTest.toString)
    JDOMExternalizer.write(element, "testName", testName)
    JDOMExternalizer.write(element, "testKind", if (testKind != null) testKind.toString else TestKind.CLASS.toString)
    JDOMExternalizer.write(element, "showProgressMessages", showProgressMessages.toString)
    PathMacroManager.getInstance(getProject).collapsePathsRecursively(element)
  }

  override def readExternal(element: Element) {
    PathMacroManager.getInstance(getProject).expandPaths(element)
    super.readExternal(element)
    JavaRunConfigurationExtensionManager.getInstance.readExternal(this, element)
    readModule(element)
    testClassPath = JDOMExternalizer.readString(element, "path")
    testPackagePath = JDOMExternalizer.readString(element, "package")
    javaOptions = JDOMExternalizer.readString(element, "vmparams")
    testArgs = JDOMExternalizer.readString(element, "params")
    workingDirectory = JDOMExternalizer.readString(element, "workingDirectory")
    val s = JDOMExternalizer.readString(element, "searchForTest")
    for (search <- SearchForTest.values()) {
      if (search.toString == s) searchTest = search
    }
    testName = Option(JDOMExternalizer.readString(element, "testName")).getOrElse("")
    testKind = TestKind.fromString(Option(JDOMExternalizer.readString(element, "testKind")).getOrElse("Class"))
    showProgressMessages = JDOMExternalizer.readBoolean(element, "showProgressMessages")
  }
}

object AbstractTestRunConfiguration {

  private[test] trait TestCommandLinePatcher {
    private var failedTests: Seq[(String, String)] = null

    def setFailedTests(failedTests: Seq[(String, String)]) {
      this.failedTests = Option(failedTests).map(_.distinct).orNull
    }

    def getFailedTests = failedTests

    def getClasses: Seq[String]

    @BeanProperty
    var configuration: RunConfigurationBase = null
  }

  private[test] trait PropertiesExtension {
    def getRunConfigurationBase: RunConfigurationBase
  }

  protected[test] def isInvalidSuite(clazz: PsiClass): Boolean = {
    val list: PsiModifierList = clazz.getModifierList
    list != null && list.hasModifierProperty("abstract") || lackNoArgConstructor(clazz)
  }

  private def lackNoArgConstructor(clazz: PsiClass): Boolean = {
    clazz match {
      case c: ScClass =>
        val constructors = c.secondaryConstructors.filter(_.isConstructor).toList ::: c.constructor.toList
        for (con <- constructors) {
          if (con.isConstructor && con.parameterList.getParametersCount == 0) {
            con match {
              case owner: ScModifierListOwner =>
                if (owner.hasModifierProperty("public")) return false
              case _ =>
            }
          }
        }

      case _ =>
        for (constructor <- clazz.getConstructors) {
          if (constructor.isConstructor && constructor.getParameterList.getParametersCount == 0) {
            constructor match {
              case owner: ScModifierListOwner =>
                if (owner.hasModifierProperty("public")) return false
              case _ =>
            }
          }
        }
    }
    true
  }
}