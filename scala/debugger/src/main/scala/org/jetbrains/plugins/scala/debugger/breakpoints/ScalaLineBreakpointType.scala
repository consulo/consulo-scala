package org.jetbrains.plugins.scala.debugger.breakpoints

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.ui.breakpoints.{Breakpoint, BreakpointManager, JavaLineBreakpointType, LineBreakpoint, RunToCursorBreakpoint}
import consulo.application.ReadAction
import consulo.execution.debug.icon.ExecutionDebugIconGroup
import consulo.language.icon.IconDescriptorUpdaters
import consulo.document.FileDocumentManager
import consulo.project.{DumbService, Project}
import consulo.document.util.TextRange
import consulo.virtualFileSystem.VirtualFile
import com.intellij.java.language.psi.{PsiClass, PsiMethod}
import consulo.language.psi.{PsiComment, PsiElement, PsiFile, PsiManager, PsiWhiteSpace}
import consulo.language.psi.util.PsiTreeUtil
import consulo.application.util.function.Processor
import consulo.annotation.access.{RequiredReadAction => RequiresReadLock}
import consulo.execution.debug.breakpoint.{XBreakpoint, XLineBreakpoint}
import consulo.execution.debug.{XDebuggerUtil, XSourcePosition}
import org.jetbrains.annotations.{NotNull, Nullable}
import com.intellij.java.debugger.impl.breakpoints.properties.JavaLineBreakpointProperties
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.debugger.{DebuggerBundle, ScalaConditionalReturn, ScalaConditionalReturnSourcePosition, ScalaLambdaSourcePosition, ScalaPositionManager, ScalaSourcePositionWithWholeLineHighlighted, typeAware}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScExtractorPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScFunctionExpr, ScTry}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScFunctionDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateBody
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.{ScEarlyDefinitions, ScNamedElement}
import org.jetbrains.plugins.scala.statistics.ScalaDebuggerUsagesCollector

import consulo.ui.image.Image

import java.util.Collections

// upstream: order = "last" (not allowed on consulo-java extension points)
@ExtensionImpl
class ScalaLineBreakpointType extends ScalaLineBreakpointTypeBase("scala-line", DebuggerBundle.message("line.breakpoints.tab.title")) {

  override def getDisplayName: String = DebuggerBundle.message("line.breakpoints.tab.title")

  // upstream: overrides `canPutAt`, in Consulo it is called by ScalaLineBreakpointTypeResolver
  @RequiresReadLock
  def canPutAt(@NotNull file: VirtualFile, line: Int, @NotNull project: Project): Boolean = {
    val psiFile = PsiManager.getInstance(project).findFile(file)
    if (psiFile == null) return false
    if (!psiFile.getLanguage.isKindOf(ScalaLanguage.INSTANCE)) return false
    val document = FileDocumentManager.getInstance.getDocument(file)
    if (document == null) return false
    psiFile match {
      case sf: ScalaFile if sf.isWorksheetFile => return false // we do not support debugging in worksheets yet
      case _ =>
    }

    var result: Boolean = false
    val processor: Processor[PsiElement] = {
      case ElementType(ScalaTokenTypes.kPACKAGE | ScalaTokenTypes.kIMPORT)  => false
      case _: PsiWhiteSpace                                                 => true
      case e if PsiTreeUtil.getParentOfType(e, classOf[PsiComment]) != null => true
      case e if PsiTreeUtil.getParentOfType(e, classOf[ScExpression], classOf[ScExtractorPattern], classOf[ScClass]) != null =>
        result = true
        false
      case _ => true
    }
    XDebuggerUtil.getInstance.iterateLine(project, document, line, processor)
    result
  }

  private type JavaBPVariant = JavaLineBreakpointType#JavaBreakpointVariant

  @RequiresReadLock
  @NotNull
  override def computeVariants(@NotNull project: Project, @NotNull position: XSourcePosition): java.util.List[JavaBPVariant] = {
    val dumbService = DumbService.getInstance(project)
    if (dumbService.isDumb) return Collections.emptyList()

    val file = PsiManager.getInstance(project).findFile(position.getFile) match {
      case null => return Collections.emptyList()
      case sf: ScalaFile => sf
      case _ => return Collections.emptyList()
    }
    val line = position.getLine

    val positionsOnLine = ScalaPositionManager.positionsOnLine(file, line)
    val lambdas = ScalaPositionManager.filterLambdasOnLine(file, line, positionsOnLine)
    val conditionalReturn = findConditionalReturn(file, line)

    if (lambdas.isEmpty && conditionalReturn.isEmpty) return Collections.emptyList()

    val res = new java.util.LinkedList[JavaBPVariant]()

    val method = Option(SourcePosition.createFromLine(file, line).getElementAt)
      .flatMap(findContainingDefinition(_, lambdas))

    val extraPriorityForLambdas = positionsOnLine.sizeCompare(lambdas) == 0
    for ((lambda, ordinal) <- lambdas.zipWithIndex) {
      val element = lambda match {
        case f: ScFunctionExpr => f.result.getOrElse(f)
        case e => e
      }
      res.addLast(new LambdaScalaBreakpointVariant(XDebuggerUtil.getInstance.createPositionByElement(element), element, ordinal, extraPriorityForLambdas))
    }
    res.addFirst(new LineScalaBreakpointVariant(position, method.orNull))
    if (lambdas.nonEmpty) {
      res.addFirst(new JavaBreakpointVariant(position)) //adding all variants
    }
    // Must stay last: on a plain gutter click the platform picks the first variant with the highest priority, and
    // this variant ties with the line variant. The line variant must win.
    conditionalReturn.foreach { returnKeyword =>
      res.addLast(new ConditionalReturnScalaBreakpointVariant(position, returnKeyword))
    }
    res
  }

  /**
   * The `return` keyword of a single conditional (early) return on this line, if any (SCL-21626).
   *
   * Only early returns from real methods are supported: scalac lowers a `return` from inside a lambda into
   * `throw new NonLocalReturnControl(...)`, so no return instruction is emitted for it. The only return opcode
   * on such a line belongs to the closure's normal exit, i.e. the breakpoint would suspend exactly when the
   * early return does '''not''' happen.
   */
  private def findConditionalReturn(file: PsiFile, line: Int): Option[PsiElement] =
    ScalaConditionalReturn.findSingleConditionalReturn(file, line)
      .filter(isReturnFromMethod)

  /**
   * Whether the `return` returns from a method, rather than non-locally from an enclosing lambda, and whether
   * a return instruction is actually emitted on its own line.
   *
   * Note that [[findContainingDefinition]] cannot be reused here: it deliberately skips the lambdas that are
   * on the line, so it would report the enclosing method for the very case we need to reject.
   */
  private def isReturnFromMethod(returnKeyword: PsiElement): Boolean =
    returnKeyword.withParentsInFile.collectFirst {
      // `typeAware = true` on purpose: recognizing a by-name argument (`opt.getOrElse { return 1 }`) needs
      // resolution, and treating one as a plain block would plant the breakpoint on the closure's own return.
      case e if ScalaPositionManager.isLambda(e, typeAware = true) => false
      // An early return out of a `try` with a `finally` only stores the result and jumps to the finally
      // block; every return instruction is emitted there, attributed to the finally body's lines. There is
      // nothing on this line to suspend on, so offering the variant would create a breakpoint that never hits.
      // A `try` with only a `catch` is fine: the return instruction stays on this line.
      case t: ScTry if t.finallyBlock.isDefined                    => false
      // `<init>` is the one method whose line numbers LocationLineManager remaps to a different line.
      case f: ScFunctionDefinition                                 => !f.isConstructor
      // Stop at the class boundary, so that a `return` in a nested initializer is not taken for a method one.
      case _: ScTemplateBody | _: ScEarlyDefinitions               => false
    }.getOrElse(false) // no enclosing method at all

  private def findContainingDefinition(elem: PsiElement, lambdas: Seq[PsiElement]): Option[PsiElement] = {
    val project = elem.getProject

    elem.withParentsInFile.collect {
      case c if ScalaPositionManager.isLambda(c, typeAware(project)) => c
      case m: PsiMethod => m
      case tb: ScTemplateBody => tb
      case ed: ScEarlyDefinitions => ed
      case c: ScClass => c
    }.find(!lambdas.contains(_))
  }

  //noinspection InstanceOf
  override def matchesPosition(@NotNull breakpoint: LineBreakpoint[?], @NotNull position: SourcePosition): Boolean = {
    val method = getContainingMethod(breakpoint)
    if (method == null) return false

    if (!breakpoint.isInstanceOf[RunToCursorBreakpoint] && isMatchAll(breakpoint)) return true

    if (isLambda(breakpoint)) {
      ScalaDebuggerUsagesCollector.logLambdaBreakpoint(breakpoint.getProject)
      if (!position.isInstanceOf[ScalaLambdaSourcePosition]) return false
      val element = position.asInstanceOf[ScalaLambdaSourcePosition].lambda
      position.isInstanceOf[ScalaLambdaSourcePosition] &&
        ScalaPositionManager.isLambda(element) && element.getTextRange == method.getTextRange
    } else {
      val element = position.getElementAt
      //a conditional return position is a line position too: the position manager only remaps it onto the
      //`return` keyword so that just the keyword is highlighted on suspend. Rejecting it here would leave
      //a line whose very first instruction is a return without any request at all.
      position.is[ScalaSourcePositionWithWholeLineHighlighted, ScalaConditionalReturnSourcePosition] &&
        element != null && position.getLine == element.getLineNumber
    }
  }

  @Nullable
  override def getContainingMethod(@NotNull breakpoint: LineBreakpoint[?]): PsiElement = {
    val position = breakpoint.getSourcePosition
    if (position == null) return null
    val element = position.getElementAt
    if (element == null) return null

    val ordinal = lambdaOrdinal(breakpoint)
    val lambdas = ScalaPositionManager.lambdasOnLine(position.getFile, position.getLine)
    if (!isLambda(breakpoint) || ordinal > lambdas.size - 1) {
      findContainingDefinition(element, lambdas).orNull
    } else lambdas(ordinal)
  }

  //noinspection ApiStatus,UnstableApiUsage
  override def getHighlightRange(breakpoint: XLineBreakpoint[JavaLineBreakpointProperties]): TextRange = {
    // upstream: the platform highlights the `return` keyword itself
    if (ScalaLineBreakpointType.isConditionalReturn(breakpoint)) return conditionalReturnOf(breakpoint).map(_.getTextRange).orNull

    BreakpointManager.getJavaBreakpoint(breakpoint) match {
      case lineBp: LineBreakpoint[_] if isLambda(lineBp) =>
        if (DumbService.getInstance(lineBp.getProject).isDumb) {
          // The lambda body range needs resolve; once indexes are ready, ask the
          // platform to recompute the range and redraw the highlighter.
          invokeWhenSmart(lineBp.getProject)(breakpoint.fireBreakpointChanged())
          null
        }
        else Option(getContainingMethod(lineBp)).map {
          case f: ScFunctionExpr => f.result.getOrElse(f).getTextRange
          case e => e.getTextRange
        }.orNull
      case _ => null
    }
  }

  override def getSourcePosition(breakpoint: XBreakpoint[JavaLineBreakpointProperties]): XSourcePosition =
    if (ScalaLineBreakpointType.isConditionalReturn(breakpoint))
      ReadAction.compute[XSourcePosition, RuntimeException] { () =>
        conditionalReturnOf(breakpoint).map(XDebuggerUtil.getInstance.createPositionByElement).orNull
      }
    else super.getSourcePosition(breakpoint)

  override protected def createScalaBreakpoint(project: Project, breakpoint: XBreakpoint[JavaLineBreakpointProperties]): Breakpoint[JavaLineBreakpointProperties] =
    new ScalaLineBreakpoint(project, breakpoint)

  private def conditionalReturnOf(breakpoint: XBreakpoint[JavaLineBreakpointProperties]): Option[PsiElement] =
    breakpoint match {
      case lineBreakpoint: XLineBreakpoint[?] if lineBreakpoint.getFile != null =>
        Option(PsiManager.getInstance(lineBreakpoint.getProject).findFile(lineBreakpoint.getFile))
          .flatMap(ScalaConditionalReturn.findSingleConditionalReturn(_, lineBreakpoint.getLine))
      case _ => None
    }

  private def lambdaOrdinal(breakpoint: LineBreakpoint[?]): Integer = {
    val xBreakpoint = breakpoint.getXBreakpoint
    if (xBreakpoint != null) {
      xBreakpoint.getProperties match {
        case jp: JavaLineBreakpointProperties => jp.getLambdaOrdinal
        case _ => null
      }
    }
    else null
  }

  private def isLambda(breakpoint: LineBreakpoint[?]): Boolean = {
    val ordinal = lambdaOrdinal(breakpoint)
    ordinal != null && ordinal >= 0
  }

  private def isMatchAll(breakpoint: LineBreakpoint[?]): Boolean = lambdaOrdinal(breakpoint) == null

  override def getPriority: Int = super.getPriority + 1

  private final class LineScalaBreakpointVariant(position: XSourcePosition, @Nullable method: PsiElement)
    extends LineJavaBreakpointVariant(position, method, -1) {

    override def getIcon: Image = method match {
      case e @ (_: PsiMethod | _: PsiClass | _: PsiFile) => IconDescriptorUpdaters.getIcon(e, 0)
      case _ => ExecutionDebugIconGroup.breakpointBreakpoint()
    }

    override def getText: String = method match {
      case c: ScClass => DebuggerBundle.message("breakpoint.location.constructor.of", c.name)
      case ed: ScEarlyDefinitions =>
        val clazz = PsiTreeUtil.getParentOfType(ed, classOf[ScTypeDefinition])
        if (clazz != null) DebuggerBundle.message("breakpoint.location.early.definitions.of", clazz.name)
        else DebuggerBundle.message("breakpoint.location.line.in.containing.block")
      case (_: ScFunction) & (named: ScNamedElement) => DebuggerBundle.message("breakpoint.location.line.in.function", named.name)
      case _: ScalaFile => DebuggerBundle.message("breakpoint.location.line.in.containing.file")
      case _ => DebuggerBundle.message("breakpoint.location.line.in.containing.block")
    }

    override def isLowPriority(firstLineElement: PsiElement): Boolean =
      firstLineElement.elementType == ScalaTokenTypes.tDOT
  }

  // upstream: JavaLineBreakpointType.ConditionalReturnJavaBreakpointVariant (lambda ordinal NO_LAMBDA with the conditional return flag)
  private final class ConditionalReturnScalaBreakpointVariant(position: XSourcePosition, element: PsiElement)
    extends ExactJavaBreakpointVariant(position, element, ScalaLineBreakpointType.ConditionalReturnOrdinal) {

    override def getIcon: Image = ExecutionDebugIconGroup.breakpointBreakpoint()
  }

  private final class LambdaScalaBreakpointVariant(position: XSourcePosition, @Nullable element: PsiElement, lambdaOrdinal: Int, hasExtraPriority: Boolean)
    extends LambdaJavaBreakpointVariant(position, element, lambdaOrdinal) {

    override def getPriority(project: Project): Int = {
      val priority = super.getPriority(project)
      val extraPriorty = if (hasExtraPriority) 50 else 0
      priority + extraPriorty
    }
  }
}

object ScalaLineBreakpointType {

  /**
   * The lambda ordinal of conditional return breakpoints.
   * upstream: JavaLineBreakpointProperties.isConditionalReturn (encoded inline position), consulo-java's
   * properties keep only the lambda ordinal (null - all positions, -1 - the line, 0 or more - a lambda)
   */
  private[breakpoints] val ConditionalReturnOrdinal: Integer = -2

  private[breakpoints] def isConditionalReturn(breakpoint: XBreakpoint[?]): Boolean =
    breakpoint != null && (breakpoint.getProperties match {
      case properties: JavaLineBreakpointProperties => properties.getLambdaOrdinal == ConditionalReturnOrdinal
      case _ => false //the properties are nullable in practice
    })
}
