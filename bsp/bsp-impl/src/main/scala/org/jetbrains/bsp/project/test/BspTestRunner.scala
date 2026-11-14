package org.jetbrains.bsp.project.test

import ch.epfl.scala.bsp4j.TaskDataKind._
import ch.epfl.scala.bsp4j.TestStatus._
import ch.epfl.scala.bsp4j._
import com.google.gson.{Gson, JsonObject}
import consulo.execution.configuration.RunProfileState
import consulo.execution.runner.ProgramRunner
import consulo.execution.test.sm.SMTestRunnerConnectionUtil
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties
import consulo.execution.{DefaultExecutionResult, ExecutionResult}
import consulo.execution.executor.Executor
import consulo.module.ModuleManager
import consulo.process.{BaseProcessHandler, ProcessHandler, ProcessOutputTypes}
import consulo.project.Project
import org.jetbrains.bsp.data.BspMetadata
import org.jetbrains.bsp.protocol.BspCommunication
import org.jetbrains.bsp.protocol.BspNotifications.{BspNotification, LogMessage, TaskFinish, TaskStart}
import org.jetbrains.bsp.protocol.session.BspSession.{BspServer, BuildServerInfo}
import org.jetbrains.bsp.{BspBundle, BspErrorMessage}
import org.jetbrains.plugins.scala.build.BuildToolWindowReporter.CancelBuildAction
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter, BuildToolWindowReporter}

import java.io.OutputStream
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

class BspTestRunner(
                     val project: Project,
                     val rc: BspTestRunConfiguration,
                     val ex: Executor,
                     val testClasses: Option[Map[URI, List[String]]]) extends RunProfileState {

  val gson = new Gson()

  class MProcHandler extends BaseProcessHandler {
    override def destroyProcessImpl(): Unit = ()

    override def detachProcessImpl(): Unit = {}

    override def detachIsDefault(): Boolean = false

    override def getProcessInput: OutputStream = null

    def shutdown(): Unit = {
      super.notifyProcessTerminated(0)
    }
  }

  private def targets(): List[URI] = {
    ModuleManager.getInstance(project).getModules.toList
      .flatMap(BspMetadata.get(project, _).toOption)
      .flatMap(x => x.targetIds.asScala.toList.map(_.uri))
  }

  private def testRequest(server: BspServer, serverInfo: BuildServerInfo): CompletableFuture[TestResult] = {
    // TODO should we check for intersection of language ids in individual targets, and whether targets are testable?
    val testsSupported = !serverInfo.capabilities.getTestProvider.getLanguageIds.isEmpty
    if (testsSupported) {
      val targetIds = targets().map(uri => new BuildTargetIdentifier(uri.toString))
      val params = new TestParams(targetIds.asJava)
      params.setOriginId(UUID.randomUUID().toString)
      testClasses match {
        case Some(m) =>
          val scalaTestClasses = m
            .map { case (uri, classes) =>
              new ScalaTestClassesItem(new BuildTargetIdentifier(uri.toString), classes.asJava) }
            .toList
          params.setDataKind("scala-test")
          params.setData({
            val p = new ScalaTestParams
            p.setTestClasses(scalaTestClasses.asJava)
            p
          })
        case None =>
      }
      server.buildTargetTest(params)
    } else {
      val result = new CompletableFuture[TestResult]()
      result.completeExceptionally(BspErrorMessage(BspBundle.message("bsp.test.build.server.does.not.support.testing")))
      result
    }
  }


  def printProc(str: ServiceMsg)(implicit proc: ProcessHandler): Unit = {
    proc.notifyTextAvailable(str.toString + System.lineSeparator(), ProcessOutputTypes.STDOUT)
  }

  // upstream: jetbrains.buildServer.messages.serviceMessages (TeamCity service messages library, not in Consulo)
  class ServiceMsg(val name: String, val map: Map[String, String]) {
    def this(n: String) = this(n, Map())

    private def escape(value: String): String =
      value.flatMap {
        case '|' => "||"
        case '\'' => "|'"
        case '\n' => "|n"
        case '\r' => "|r"
        case '[' => "|["
        case ']' => "|]"
        case c => c.toString
      }

    override def toString: String = {
      val attributes = map.collect { case (key, value) if value != null => s" $key='${escape(value)}'" }.mkString
      s"##teamcity[$name$attributes]"
    }
  }

  private def Message(text: String, status: String) = new ServiceMsg("message", Map("text" -> text, "status" -> status))
  private def TestSuiteStarted(name: String) = new ServiceMsg("testSuiteStarted", Map("name" -> name))
  private def TestSuiteFinished(name: String) = new ServiceMsg("testSuiteFinished", Map("name" -> name))
  private def TestStarted(name: String) = new ServiceMsg("testStarted", Map("name" -> name, "captureStandardOutput" -> "false"))
  private def TestIgnored(name: String, comment: String) = new ServiceMsg("testIgnored", Map("name" -> name, "message" -> comment))
  private def TestFinished(name: String, duration: Int) = new ServiceMsg("testFinished", Map("name" -> name, "duration" -> duration.toString))

  case class BspMessageNestedData(dataKind: String, data: AnyRef)

  private def extractNestedMessage(bspMessageData: BspMessageNestedData): AnyRef = {
    val kind = bspMessageData.dataKind
    val rawNested = bspMessageData.data
    rawNested match {
      case d: JsonObject => kind match {
        case TEST_FINISH => gson.fromJson(d, classOf[TestFinish])
        case TEST_TASK => gson.fromJson(d, classOf[TestTask])
        case TEST_REPORT => gson.fromJson(d, classOf[TestReport])
        case TEST_START => gson.fromJson(d, classOf[TestStart])
        case _ => rawNested
      }
      case _ => rawNested
    }
  }

  class BspTestSession {
    val startTime: mutable.Map[String, Long] = mutable.Map()
  }

  private def onBspNotification(proc: ProcessHandler, session: BspTestSession)(n: BspNotification): Unit = {
    implicit val pr: ProcessHandler = proc
    n match {
      case LogMessage(params) => // TODO associate log messages with the running test correctly
        printProc(Message(params.getMessage + System.lineSeparator(), "NORMAL"))
      case TaskStart(params) => extractNestedMessage(BspMessageNestedData(params.getDataKind, params.getData)) match {
        case t: TestTask =>
          printProc(new ServiceMsg("enteredTheMatrix"))
          printProc(TestSuiteStarted("BSP"))
        case t: TestStart =>
          printProc(TestStarted(t.getDisplayName))
          session.startTime += (t.getDisplayName -> params.getEventTime)
        case _ =>
      }
      case TaskFinish(params) => extractNestedMessage(BspMessageNestedData(params.getDataKind, params.getData)) match {
        case t: TestReport => printProc(TestSuiteFinished("BSP"))
        case t: TestFinish =>
          t.getStatus match {
            case PASSED =>
            case FAILED => printProc(new ServiceMsg("testFailed", Map("name" -> t.getDisplayName, "message" -> t.getMessage)))
            case _ => printProc(TestIgnored(t.getDisplayName, "Ignored"))
          }
          printProc(TestFinished(t.getDisplayName, session.startTime.get(t.getDisplayName)
            .map { st =>
              session.startTime -= t.getDisplayName
              (params.getEventTime - st).intValue()
            }.getOrElse(0)))
        case _ =>
      }
      case _ =>
    }

  }


  override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
    val procHandler = new MProcHandler
    val console = SMTestRunnerConnectionUtil.createAndAttachConsole("BSP", procHandler, new SMTRunnerConsoleProperties(
      rc, "BSP", ex))
    val bspCommunication = BspCommunication.forWorkspace(Path.of(project.getBasePath), project)

    val cancelToken = Promise[Unit]()
    val cancelAction = new CancelBuildAction(cancelToken, indicator = None)
    implicit val reporter: BuildReporter = new BuildToolWindowReporter(project, BuildMessages.randomEventId, BspBundle.message("bsp.tests.reporter.title"), cancelAction)
    reporter.start()

    bspCommunication
      .run(testRequest, onBspNotification(procHandler, new BspTestSession()), reporter.log)
      .future
      .onComplete { res =>
        res match {
          case Success(testResult) =>
            reporter.finish(BuildMessages.empty.status(BuildMessages.OK))
          case Failure(x) =>
            reporter.finishWithFailure(x)
        }
        procHandler.shutdown()
      }
    new DefaultExecutionResult(console, procHandler)
  }

}
