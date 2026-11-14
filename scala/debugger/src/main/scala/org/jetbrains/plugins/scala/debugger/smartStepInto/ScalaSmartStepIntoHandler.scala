package org.jetbrains.plugins.scala.debugger.smartStepInto

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.actions.{JvmSmartStepIntoHandler, MethodSmartStepTarget, SmartStepTarget}
import com.intellij.java.debugger.impl.PrioritizedTask.Priority
import com.intellij.java.debugger.impl.{DebuggerSession, PrioritizedTask}
import com.intellij.java.debugger.impl.engine.events.DebuggerContextCommandImpl
import com.intellij.java.debugger.impl.engine.{MethodFilter, SuspendContextImpl}
import consulo.application.ReadAction
import consulo.document.FileDocumentManager
import consulo.document.util.TextRange
import consulo.language.psi.{PsiElement, PsiFile}
import com.intellij.java.language.psi.PsiMethod
import consulo.language.psi.util.PsiTreeUtil
import consulo.util.lang.Range
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.codeInspection.collections.{MethodRepr, stripped}
import org.jetbrains.plugins.scala.debugger.filters.ScalaDebuggerSettings
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScParameterizedTypeElement, ScSimpleTypeElement, ScTypeElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScConstructorInvocation, ScMethodLike}
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaFile, ScalaRecursiveElementVisitor}
import org.jetbrains.plugins.scala.statistics.ScalaDebuggerUsagesCollector
import org.jetbrains.plugins.scala.util.AnonymousFunction

import java.util.Collections
import java.util.concurrent.CompletableFuture
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

@ExtensionImpl
class ScalaSmartStepIntoHandler extends JvmSmartStepIntoHandler {

  override def findSmartStepTargets(position: SourcePosition): java.util.List[SmartStepTarget] = {
    throw new IllegalStateException("Should not be used")
  }

  // upstream: Promise; "Step Into" of the Consulo platform does not ask for these targets yet
  override def findStepIntoTargets(position: SourcePosition, session: DebuggerSession): CompletableFuture[java.util.List[SmartStepTarget]] = {
    if (ScalaDebuggerSettings.getInstance().ALWAYS_SMART_STEP_INTO) {
      findSmartStepTargetsAsync(position, session)
    }
    else {
      CompletableFuture.failedFuture(new UnsupportedOperationException())
    }
  }

  override def findSmartStepTargetsAsync(position: SourcePosition, session: DebuggerSession): CompletableFuture[java.util.List[SmartStepTarget]] = {
    val res = new CompletableFuture[java.util.List[SmartStepTarget]]()
    val context = session.getContextManager.getContext
    context.getDebugProcess.getManagerThread.schedule(new DebuggerContextCommandImpl(context) {
      override def threadAction(suspendContext: SuspendContextImpl): Unit = {
        try res.complete(ReadAction.compute(() => doFindSmartStepTargets(position)))
        catch {
          case t: Throwable =>
            res.completeExceptionally(t)
            if (!NonFatal(t)) throw t
        }
      }

      override def commandCancelled(): Unit = {
        res.cancel(false)
      }

      override def getPriority: PrioritizedTask.Priority = Priority.NORMAL
    })
    res
  }

  @TestOnly
  private[smartStepInto] def findSmartStepTargetsInTests(position: SourcePosition): java.util.List[SmartStepTarget] =
    doFindSmartStepTargets(position)

  private def doFindSmartStepTargets(position: SourcePosition): java.util.List[SmartStepTarget] = {
    val line: Int = position.getLine
    if (line < 0) {
      return Collections.emptyList[SmartStepTarget]()
    }

    val scalaFile = Option(position.getFile) match {
      case Some(sf: ScalaFile) if !sf.isCompiled => sf
      case _ => return Collections.emptyList[SmartStepTarget]()
    }

    ScalaDebuggerUsagesCollector.logSmartStepInto(scalaFile.getProject)

    val document = Option(scalaFile.getVirtualFile).flatMap { vFile =>
      Option(FileDocumentManager.getInstance().getDocument(vFile))
    }.filter(_.getLineCount > line) match {
      case Some(doc) => doc
      case None => return Collections.emptyList[SmartStepTarget]()
    }

    val element = position.getElementAt

    val lineStart = element.getTextRange.getStartOffset
    val lineRange = new TextRange(lineStart, document.getLineEndOffset(line))
    val maxElement = maxElementOnLine(element, lineStart)

    def intersectsWithLineRange(elem: PsiElement): Boolean =
      lineRange.intersects(elem.getTextRange)

    val lineToSkip = new Range[Integer](line, line)
    val collector = new TargetCollector(lineToSkip, intersectsWithLineRange)
    maxElement.accept(collector)
    maxElement.nextSiblings
            .takeWhile(intersectsWithLineRange)
            .foreach(_.accept(collector))
    collector.result.sortBy(_.getHighlightElement.getTextOffset).asJava
  }

  override def isAvailable(position: SourcePosition): Boolean = {
    val file: PsiFile = position.getFile
    file.isInstanceOf[ScalaFile]
  }

  override def createMethodFilter(stepTarget: SmartStepTarget): MethodFilter = {
    stepTarget match {
      case methodTarget: MethodSmartStepTarget =>
        val scalaFilter = methodTarget.getMethod match {
          case f @ (_: ScMethodLike | _: FakeAnonymousClassConstructor) if stepTarget.needsBreakpointRequest() =>
            ScalaBreakpointMethodFilter.from(f, stepTarget.getCallingExpressionLines)
          case fun: ScMethodLike =>
            Some(new ScalaMethodFilter(fun, stepTarget.getCallingExpressionLines))
          case _ => None
        }
        scalaFilter.getOrElse(super.createMethodFilter(stepTarget))
      case ScalaFunExprSmartStepTarget(_, stmts) =>
        ScalaBreakpointMethodFilter.from(None, stmts, stepTarget.getCallingExpressionLines)
                .getOrElse(super.createMethodFilter(stepTarget))
      case _ => super.createMethodFilter(stepTarget)
    }
  }

  @tailrec
  private def maxElementOnLine(startElem: PsiElement, lineStart: Int): PsiElement = {
    val parent = startElem.getParent
    parent match {
      case _: ScBlock | null => startElem
      case p if p.getTextRange.getStartOffset >= lineStart => maxElementOnLine(parent, lineStart)
      case _ => startElem
    }
  }

  private class TargetCollector(noStopAtLines: Range[Integer], elementFilter: PsiElement => Boolean) extends ScalaRecursiveElementVisitor {
    val result: ArrayBuffer[SmartStepTarget] = ArrayBuffer[SmartStepTarget]()

    override def visitNewTemplateDefinition(templ: ScNewTemplateDefinition): Unit = {
      if (!elementFilter(templ)) return

      val extBl = templ.extendsBlock
      var label = ""

      def findConstructorInvocation(typeElem: ScTypeElement): Option[ScConstructorInvocation] = typeElem match {
        case p: ScParameterizedTypeElement => p.findConstructorInvocation
        case s: ScSimpleTypeElement => s.findConstructorInvocation
        case _ => None
      }

      def addConstructor(): Unit = {
        for {
          tp <- extBl.templateParents
          typeElem <- tp.typeElements.headOption
          constrInvocation <- findConstructorInvocation(typeElem)
          ref <- constrInvocation.reference
        } {
          label = constrInvocation.simpleTypeElement.fold("")(ste => s"new ${ste.getText}.")

          val generateAnonClass = AnonymousFunction.generatesAnonClass(templ)
          val method = ref.resolve() match {
            case m: PsiMethod if !generateAnonClass => m
            case _ => new FakeAnonymousClassConstructor(templ, ref.refName)
          }
          result += new MethodSmartStepTarget(method, "new ", constrInvocation, /*needBreakpointRequest = */ generateAnonClass, noStopAtLines)
        }
      }

      def addMethodsIfInArgument(): Unit = {
        PsiTreeUtil.getParentOfType(templ, classOf[MethodInvocation]) match {
          case MethodRepr(_, _, _, args) if args.map(stripped).contains(templ) =>
            extBl.templateBody match {
              case Some(tb) =>
                for {
                  case fun @ (_f: ScFunctionDefinition) <- tb.functions
                  body <- fun.body
                } {
                  result += new MethodSmartStepTarget(fun, label, body, true, noStopAtLines)
                }
              case _ =>
            }
          case _ =>
        }
      }

      addConstructor()
      addMethodsIfInArgument()
    }

    override def visitExpression(expr: ScExpression): Unit = {
      if (!elementFilter(expr)) return

      expr.implicitElement().collect {
        case method: PsiMethod => method
      }.filter(_.isPhysical) // synthetic conversions are created for implicit classes
        .map(new MethodSmartStepTarget(_, "implicit ", expr, false, noStopAtLines))
        .foreach(result.+=)

      expr match {
        case ScalaPsiUtil.MethodValue(m) =>
          result += new MethodSmartStepTarget(m, null, expr, true, noStopAtLines)
          return
        case FunExpressionTarget(stmts, presentation) =>
          result += new ScalaFunExprSmartStepTarget(expr, stmts, presentation, noStopAtLines)
          return //stop at function expression
        case ref: ScReferenceExpression =>
          ref.resolve() match {
            case fun: ScFunctionDefinition if fun.isApplyMethod && ref.refName != "apply" =>
              val prefix = s"${ref.refName}."
              result += new MethodSmartStepTarget(fun, prefix, ref.nameId, false, noStopAtLines)
            case (f: ScFunctionDefinition) & ContainingClass(cl: ScClass) if cl.getModifierList.hasModifierProperty("implicit") =>
              val isActuallyImplicit = ref.qualifier.flatMap(_.implicitElement()).isDefined
              val prefix = if (isActuallyImplicit) "implicit " else null
              result += new MethodSmartStepTarget(f, prefix, ref.nameId, false, noStopAtLines)
            case fun: PsiMethod =>
              result += new MethodSmartStepTarget(fun, null, ref.nameId, false, noStopAtLines)
            case _ =>
          }
        case _ =>
      }
      super.visitExpression(expr)
    }

    override def visitPattern(pat: ScPattern): Unit = {
      if (!elementFilter(pat)) return

      val ref = pat match {
        case cp: ScExtractorPattern =>  Some(cp.ref)
        case _ => None
      }
      ref match {
        case Some(r @ ResolvesTo(f: ScFunctionDefinition)) =>
          val prefix = s"${r.refName}."
          result += new MethodSmartStepTarget(f, prefix, r.nameId, false, noStopAtLines)
        case _ =>
      }
      super.visitPattern(pat)
    }
  }

}
