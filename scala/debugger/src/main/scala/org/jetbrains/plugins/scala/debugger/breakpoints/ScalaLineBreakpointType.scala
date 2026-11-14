package org.jetbrains.plugins.scala.debugger.breakpoints

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.ui.breakpoints.{BreakpointManager, JavaLineBreakpointType, LineBreakpoint, RunToCursorBreakpoint}
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
import consulo.execution.debug.breakpoint.XLineBreakpoint
import consulo.execution.debug.{XDebuggerUtil, XSourcePosition}
import org.jetbrains.annotations.{NotNull, Nullable}
import com.intellij.java.debugger.impl.breakpoints.properties.JavaLineBreakpointProperties
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.debugger.{DebuggerBundle, ScalaLambdaSourcePosition, ScalaPositionManager, ScalaSourcePositionWithWholeLineHighlighted, typeAware}
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

@ExtensionImpl(order = "last")
class ScalaLineBreakpointType extends JavaLineBreakpointType("scala-line", DebuggerBundle.message("line.breakpoints.tab.title")) {

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
    if (lambdas.isEmpty) return Collections.emptyList()

    val res = new java.util.LinkedList[JavaBPVariant]()

    val method = Option(SourcePosition.createFromLine(file, line).getElementAt)
      .flatMap(findContainingDefinition(_, lambdas))

    for ((lambda, ordinal) <- lambdas.zipWithIndex) {
      val element = lambda match {
        case f: ScFunctionExpr => f.result.getOrElse(f)
        case e => e
      }
      res.addLast(new LambdaScalaBreakpointVariant(XDebuggerUtil.getInstance.createPositionByElement(element), element, ordinal))
    }
    res.addFirst(new LineScalaBreakpointVariant(position, method.orNull))
    // upstream: conditional return breakpoint variant (SCL-21626) is not supported by consulo-java
    res.addFirst(new JavaBreakpointVariant(position)) //adding all variants
    res
  }

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
      position.is[ScalaSourcePositionWithWholeLineHighlighted] &&
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
  }

  // upstream: variant priorities are not supported by the Consulo platform
  private final class LambdaScalaBreakpointVariant(position: XSourcePosition, @Nullable element: PsiElement, lambdaOrdinal: Int)
    extends LambdaJavaBreakpointVariant(position, element, lambdaOrdinal)
}
