package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.java.execution.CommonJavaRunConfigurationParameters
import com.intellij.java.execution.impl.ConfigurationWithCommandLineShortener
import com.intellij.java.execution.ShortenCommandLine
import consulo.execution.executor.Executor
import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager
import consulo.execution.configuration.{ConfigurationFactory, RunConfigurationBase, RunProfileState}
import com.intellij.java.execution.configurations.JavaRunConfigurationModule
import consulo.execution.RuntimeConfigurationException
import consulo.execution.runner.ExecutionEnvironment
import consulo.execution.configuration.fragment.RunConfigurationFragment
import consulo.execution.test.sm.runner.SMRunnerConsolePropertiesProvider
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties
import consulo.application.ReadAction
import consulo.project.`macro`.ProjectPathMacroManager
import consulo.logging.Logger
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.scope.GlobalSearchScope
import consulo.util.xml.serializer.XmlSerializer
import consulo.util.xml.serializer.annotation.Transient
import org.jdom.Element
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions.{LoggerExt, ObjectExt}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}
import org.jetbrains.plugins.scala.runner.ScalaTargetAwareRunConfiguration
import org.jetbrains.plugins.scala.testingSupport.TestingSupportBundle
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration._
import org.jetbrains.plugins.scala.testingSupport.test.munit.DelegateCommonJavaRunConfigurationParameters
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{ClassTestData, TestConfigurationData}
import org.jetbrains.plugins.scala.util.JdomExternalizerMigrationHelper
import org.jetbrains.sbt.SbtUtil.SbtProjectUriAndId

import java.{util => ju}
import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

/**
 * We need to implement [[CommonJavaRunConfigurationParameters]] to automatically enable "Run with Profiler" action. See:
 *  - [[com.intellij.profiler.ultimate.DefaultJavaProfilerStarterExtension.canRun]]
 *  - [[com.intellij.profiler.ultimate.jfr.JFRConfigurationExtension.isApplicableFor]]
 *  - SCL-21891
 */
abstract class AbstractTestRunConfiguration(
  project: Project,
  val configurationFactory: ConfigurationFactory,
  val name: String
) extends ScalaTargetAwareRunConfiguration[JavaRunConfigurationModule, Element](
  name,
  new JavaRunConfigurationModule(project, true),
  configurationFactory
) with ConfigurationWithCommandLineShortener
  with SMRunnerConsolePropertiesProvider
  with CommonJavaRunConfigurationParameters
  with DelegateCommonJavaRunConfigurationParameters //see scaladoc
  with exceptions {

  override protected def delegateToTestData: TestConfigurationData = testConfigurationData

  @Nullable
  override def createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties = {
    val hideTestRunnerConsole = this.testConfigurationData.useSbt && !this.testConfigurationData.useUiWithSbt
    // CWM-1987
    if (hideTestRunnerConsole) null
    else new ScalaTestFrameworkConsoleProperties(this, testFramework.getName, executor)
  }

  override protected def alternativeJrePath: Option[String] = Option(getAlternativeJrePath)

  def configurationProducer: AbstractTestConfigurationProducer[?]
  def testFramework: AbstractTestFramework

  private lazy val suitePaths: Seq[String] = testFramework.baseSuitePaths
  final def javaSuitePaths: ju.List[String] = suitePaths.asJava

  // TODO: move to test data itself to avoid information duplication (currently same info is expressed via testKind and testConfigurationData.class
  @BeanProperty
  var testKind: TestKind = TestKind.CLAZZ

  var testConfigurationData: TestConfigurationData = new ClassTestData(this)

  private var generatedName: String = ""
  def setGeneratedName(name: String): Unit = generatedName = name
  override def isGeneratedName: Boolean = getName == generatedName
  override def suggestedName: String = generatedName

  @Transient
  override def getShortenCommandLine: ShortenCommandLine = testConfigurationData.shortenClasspath
  override def setShortenCommandLine(mode: ShortenCommandLine): Unit = testConfigurationData.shortenClasspath = mode

  // upstream: ModuleBasedConfiguration.setModule is @Transient, Consulo's is not; the module is stored by ModuleBasedConfiguration
  @Transient
  final def getModule: Module =
    getConfigurationModule.getModule

  override def getValidModules: java.util.Collection[Module] =
    getProject.modulesWithScala.asJava

  override final def getModules: Array[Module] =
    modulesToBuildBeforeRun

  def initFrom(from: AbstractTestRunConfiguration): Unit = {
    this.setGeneratedName(from.suggestedName)
    this.setFileOutputPath(from.getOutputFilePath)
    this.setModule(from.getModule)
    this.setName(from.getName)
    this.setNameChangedByUser(!from.isGeneratedName)
    this.setSaveOutputToFile(from.isSaveOutputToFile)
    this.setShowConsoleOnStdErr(from.isShowConsoleOnStdErr)
    this.setShowConsoleOnStdOut(from.isShowConsoleOnStdOut)
    this.testConfigurationData = from.testConfigurationData.copy(this)
    this.testKind = from.testKind
  }

  /**
   * dependencies are added to scope automatically via [[GlobalSearchScopes.executionScope]]
   * @see [[com.intellij.execution.configurations.CommandLineState]]
   * @see [[getSearchScope]]
   */
  private def modulesToBuildBeforeRun: Array[Module] =
    if (testConfigurationData.searchTestsInWholeProject)
      Array.empty // empty array means for full project
    else
      super.getModules

  /** upstream: overrides ModuleRunProfile.getSearchScope (ExecutionSearchScopes.executionScope of the modules) */
  def getSearchScope: GlobalSearchScope = {
    val modules = getModules
    if (modules.isEmpty)
      notBuildModulesScope(getProject)
    else
      GlobalSearchScope.union(modules.map(GlobalSearchScope.moduleRuntimeScope(_, true)))
  }

  private def notBuildModulesScope(project: Project): GlobalSearchScope = {
    val notBuildModules = project.modules.filterNot(_.isBuildModule)
    if (notBuildModules.nonEmpty)
      GlobalSearchScope.union(notBuildModules.map(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope).toArray)
    else
      GlobalSearchScope.EMPTY_SCOPE
  }

  private def configurationScope: GlobalSearchScope =
    getModule match {
      case null   => projectScope(getProject)
      case module => moduleScope(module)
    }

  @Nullable
  private def findBaseTestSuiteClass(testClassFqn: String): PsiClass = {
    val classes = findClassInReadAction(testClassFqn)

    // base test suite can only be a class or a trait, not an object
    val nonObjects = classes.filterNot(_.is[ScObject])
    if (nonObjects.nonEmpty) nonObjects(0)
    else null
  }

  @Nullable
  protected[test] def findValidTestSuiteClassOrObject(testClassFqn: String): PsiClass = {
    val classes = findClassInReadAction(testClassFqn).toSeq
    findValidTestSuiteClassOrObject(classes, checkBaseTestSuiteIfAmbigous = true)
  }

  @tailrec
  private def findValidTestSuiteClassOrObject(classes: Seq[PsiClass], checkBaseTestSuiteIfAmbigous: Boolean): PsiClass = {
    val (objects, nonObjects) = classes.partition(_.is[ScObject])

    if (objects.nonEmpty && nonObjects.nonEmpty && checkBaseTestSuiteIfAmbigous) {
      // If both companion class and object exist, we need to make sure we use the one that actually extends the base test suite
      // This can happen, for example, in uTest.
      // Even if the actual test suite is "object", the "testClassFqn" used to represent it will be the qualified name of the companion class (without trailing `$`)
      // This is how it's done in the uTest itself.
      val suiteClass = getSuiteClass.toOption
      val classesWithValidBaseSuite = classes.filter(c => suiteClass.forall(base => validityChecker.isValidSuite(c, base)))
      findValidTestSuiteClassOrObject(classesWithValidBaseSuite, checkBaseTestSuiteIfAmbigous = false)
    }
    else if (objects.nonEmpty)
      objects.head
    else if (nonObjects.nonEmpty)
      nonObjects.head
    else
      null
  }

  private def findClassInReadAction(testClassFqn: String): Array[PsiClass] = {
    val scope = configurationScope
    ReadAction
      .nonBlocking[Array[PsiClass]](() => ScalaPsiManager.instance(project).getCachedClasses(scope, testClassFqn))
      .expireWhen(() => project.isDisposed)
      .executeSynchronously()
  }

  // TODO: when checking suite on validity we search inheritors here and in validation, extra work
  protected[test] def getSuiteClass: Either[RuntimeConfigurationException, PsiClass] = {
    val suiteClasses = suitePaths.map(findBaseTestSuiteClass).filter(_ != null)

    if (suiteClasses.isEmpty)
      Left(configurationException(TestingSupportBundle.message("test.framework.is.not.specified", testFramework.getName)))
    else if (suiteClasses.size > 1)
      Left(configurationException(TestingSupportBundle.message("test.run.config.multiple.suite.traits.detected", suiteClasses)))
    else
      Right(suiteClasses.head)
  }

  override def checkConfiguration(): Unit = {
    testConfigurationData.checkSuiteAndTestName match {
      case Left(ex) => throw ex
      case Right(_) =>
    }
    JavaRunConfigurationExtensionManager.checkConfigurationIsValid(thisConfiguration)
  }

  /**
   * upstream: settings editor group with the test configuration form, the java run configuration extensions and the logs tab.
   * In Consulo the common java options, the extensions and the logs are platform run configuration fragments.
   */
  override def createFragments(): ju.List[RunConfigurationFragment[?]] =
    ju.List.of(new AbstractTestRunConfigurationFragment)

  private def thisConfiguration: RunConfigurationBase = this

  protected[test] final def isValidSuite(clazz: PsiClass): Boolean = {
    val suiteClass = getSuiteClass
    suiteClass.fold(
      exception => {
        Log.traceSafe(s"isValidSuite: false (${exception.getMessage})")
        false
      },
      validityChecker.isValidSuite(clazz, _)
    )
  }

  /** @return whether `clazz` can be discovered when run indirectly, e.g. using "All in package" test kind */
  protected[test] def canBeDiscovered(clazz: PsiClass): Boolean = true

  protected[test] final def isInvalidSuite(clazz: PsiClass): Boolean =
    !isValidSuite(clazz)

  protected def validityChecker: SuiteValidityChecker

  override final def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState ={
    val provider = runStateProvider
    provider.commandLineState(env, None)
  }

  def runStateProvider: RunStateProvider

  override def writeExternal(element: Element): Unit = {
    // upstream: also writes the java run configuration extensions
    super.writeExternal(element)
    XmlSerializer.serializeInto(this, element)
    testConfigurationData.writeExternal(element)
    ProjectPathMacroManager.getInstance(getProject).collapsePathsRecursively(element)
  }

  override def readExternal(element: Element): Unit = {
    ProjectPathMacroManager.getInstance(getProject).expandPaths(element)
    // upstream: also reads the java run configuration extensions
    super.readExternal(element)
    XmlSerializer.deserializeInto(this, element)
    //TODO: since UserDataHolderBase extends AtomicReference in 2021.1 deserialization of it's value doesn't work and plain
    // Object instance is deserialized instead of KeyFMap
    // TODO: Around 253.12201, AbstractTestRunConfiguration no longer transitively extends AtomicReference.
    //       Figure out if we need to make larger changes.
    // set(KeyFMap.EMPTY_MAP)
    migrate(element)
    testConfigurationData = TestConfigurationData.createFromExternal(element, this)
  }

  private def migrate(element: Element): Unit =
    JdomExternalizerMigrationHelper(element) { helper =>
      helper.migrateString("testKind")(x => testKind = TestKind.parse(x))
    }
}

object AbstractTestRunConfiguration {

  private val Log = Logger.getInstance(getClass)

  case class SettingEntry(
    settingName: String,
    sbtProjectUriAndId: Option[SbtProjectUriAndId],
  )

  type SettingMap = Map[SettingEntry, String]

  def SettingMap(): SettingMap = Map[SettingEntry, String]()

   /**
    * Returns the search scope that includes transitive, non-exported dependencies and allow <code>RUNTIME</code> dependencies ([[com.intellij.psi.search.GlobalSearchScope#moduleRuntimeScope]]). </br>
    * Previously used scope ([[com.intellij.psi.search.GlobalSearchScope#moduleWithDependenciesAndLibrariesScope]]) did not allow for either of these two things.
    * <ol>
    *   <li>
    *   Regarding the <code>RUNTIME</code> scope which was in theory prohibited in ([[com.intellij.psi.search.GlobalSearchScope#moduleWithDependenciesAndLibrariesScope]])
    *   but is allowed in [[com.intellij.psi.search.GlobalSearchScope#moduleRuntimeScope]].
    *   It is prohibited in [[com.intellij.psi.search.GlobalSearchScope#moduleWithDependenciesAndLibrariesScope]] only if <code>shouldAddRuntimeDependenciesToTestCompilationClasspath</code> is false.<br>
    *   In our current codebase <code>shouldAddRuntimeDependenciesToTestCompilationClasspath</code> is always true,
    *   so this check wasn't valid.
    *   Recheck the behavior with <a href="https://youtrack.jetbrains.com/issue/SCL-22835/shouldAddRuntimeDependenciesToTestCompilationClasspath-should-be-set-to-false-if-separate-modules-for-prod-test-are-enabled">SCL-22835</a>
    *   </li>
    *   <li>
    *     Transitive dependencies will be processed only if any of the module's enumerator handler has <code>shouldProcessDependenciesRecursively</code> set to <code>true</code>
    *     (in practice, such situation occurs when separate modules for production and test are enabled, and there is a parent aka grouping module).
    *     Module's dependencies are processed recursively to one level of depth - the direct dependencies of the module and the dependencies of these dependencies.
    *     To understand why there is no indefinite recursive processing, check [[org.jetbrains.sbt.execution.SbtOrderEnumeratorHandler.getAddDependencyType]]
    *   </li>
    * </ol>
 *
 */
  private def moduleScope(module: Module): GlobalSearchScope =
    GlobalSearchScope.moduleRuntimeScope(module, true)

  /**
   * TODO: why not using just [[GlobalSearchScope.projectScope]]?
   */
  private def projectScope(project: Project): GlobalSearchScope = {
    val projectModules = ModuleManager.getInstance(project).getModules
    projectModules.iterator
      .map(moduleScope)
      .foldLeft(GlobalSearchScope.EMPTY_SCOPE)(_.union(_))
  }
}
