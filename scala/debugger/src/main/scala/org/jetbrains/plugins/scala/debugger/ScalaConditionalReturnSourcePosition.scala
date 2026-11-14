package org.jetbrains.plugins.scala.debugger

import com.intellij.java.debugger.SourcePosition
import consulo.application.ReadAction
import consulo.document.util.DocumentUtil
import consulo.execution.debug.XDebuggerUtil
import consulo.internal.com.sun.jdi.{Location, Method}
import consulo.language.psi.{PsiComment, PsiElement, PsiFile, PsiWhiteSpace}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
 * A position remapped onto the `return` keyword of a conditional (early) return, so that only the keyword is
 * highlighted when the debugger suspends there (SCL-21626).
 *
 * @see org.jetbrains.plugins.scala.debugger.ScalaSourcePositionHighlighter
 */
private final class ScalaConditionalReturnSourcePosition(delegate: SourcePosition)
  extends AbstractScalaSourcePosition(delegate)

/**
 * upstream: JavaLineBreakpointType.findSingleConditionalReturn / collectInlineConditionalReturnLocations and
 * BytecodeInspectorImpl.isConditionalReturnLocation of the IntelliJ Java debugger, not in consulo-java
 */
private[debugger] object ScalaConditionalReturn {

  /**
   * The single `return` keyword on the line, if something else precedes it on the line
   */
  def findSingleConditionalReturn(file: PsiFile, line: Int): Option[PsiElement] = ReadAction.compute[Option[PsiElement], RuntimeException] { () =>
    Option(file.getViewProvider.getDocument).filter(DocumentUtil.isValidLine(line, _)).flatMap { document =>
      var somethingBeforeReturn = false
      var singleReturn: Option[PsiElement] = None
      XDebuggerUtil.getInstance.iterateLine(file.getProject, document, line, { (element: PsiElement) =>
        var continue = true
        if (element.elementType == ScalaTokenTypes.kRETURN) {
          if (singleReturn.isDefined || !somethingBeforeReturn) {
            singleReturn = None
            continue = false
          } else {
            singleReturn = Some(element)
          }
        }
        if (!element.is[PsiWhiteSpace, PsiComment]) {
          somethingBeforeReturn = true
        }
        continue
      })
      singleReturn
    }
  }

  def canInspectBytecode(location: Location): Boolean = {
    val vm = location.virtualMachine
    vm.canGetBytecodes && vm.canGetConstantPool
  }

  /**
   * Whether the location is a return instruction, but not the final implicit return of the method
   */
  def isConditionalReturnLocation(location: Location): Boolean =
    Option(location.method).exists { method =>
      val bytecodes = method.bytecodes
      val offset = Math.toIntExact(location.codeIndex)
      offset >= 0 && offset < bytecodes.length - 1 && isReturn(bytecodes(offset) & 0xFF)
    }

  /**
   * The return instructions of the line (the 1-based line of the line number table), without the implicit return at
   * the end of the method
   */
  def inlineConditionalReturnLocations(method: Method, lineNumber: Int): Seq[Location] = {
    val returnOffsets = mutable.ArrayBuffer.empty[Int]
    var lastAddedReturnIsLastInstruction = false
    // upstream visits the bytecode with ASM and MethodBytecodeUtil.InstructionOffsetReader (not in consulo-java),
    // here the instructions are decoded directly and the lines come from the line number table
    val sortedLineStarts = method.allLineLocations.asScala.toSeq
      .map(location => location.codeIndex.toInt -> location.lineNumber)
      .sortBy(_._1)

    def lineOf(offset: Int): Int =
      sortedLineStarts.takeWhile(_._1 <= offset).lastOption.map(_._2).getOrElse(-1)

    val bytecodes = method.bytecodes
    var offset = 0
    while (offset < bytecodes.length) {
      val opcode = bytecodes(offset) & 0xFF
      lastAddedReturnIsLastInstruction = false
      if (isReturn(opcode) && lineOf(offset) == lineNumber) {
        returnOffsets += offset
        lastAddedReturnIsLastInstruction = true
      }
      offset += instructionLength(bytecodes, offset)
    }
    if (lastAddedReturnIsLastInstruction && returnOffsets.size >= 2) {
      // Return at the end of the method is likely to be implicitly generated,
      // it is not the conditional return we were looking for, drop it.
      returnOffsets.remove(returnOffsets.size - 1)
    }
    returnOffsets.toSeq.flatMap(offset => Option(method.locationOfCodeIndex(offset)))
  }

  // JVM opcodes (JVMS 6.5)
  private final val IReturn = 172
  private final val Return = 177

  private def isReturn(opcode: Int): Boolean =
    IReturn <= opcode && opcode <= Return

  private def int(bytecodes: Array[Byte], offset: Int): Int =
    ((bytecodes(offset) & 0xFF) << 24) | ((bytecodes(offset + 1) & 0xFF) << 16) |
      ((bytecodes(offset + 2) & 0xFF) << 8) | (bytecodes(offset + 3) & 0xFF)

  private def instructionLength(bytecodes: Array[Byte], offset: Int): Int =
    (bytecodes(offset) & 0xFF) match {
      case 170 => // tableswitch
        val operands = offset + 1 + (3 - offset % 4)
        val low = int(bytecodes, operands + 4)
        val high = int(bytecodes, operands + 8)
        operands - offset + 12 + (high - low + 1) * 4
      case 171 => // lookupswitch
        val operands = offset + 1 + (3 - offset % 4)
        val pairs = int(bytecodes, operands + 4)
        operands - offset + 8 + pairs * 8
      case 196 => // wide
        if ((bytecodes(offset + 1) & 0xFF) == 132 /* iinc */) 6 else 4
      case 16 | 18 | 188 | 169 => 2 // bipush, ldc, newarray, ret
      case opcode if 21 <= opcode && opcode <= 25 => 2 // iload .. aload
      case opcode if 54 <= opcode && opcode <= 58 => 2 // istore .. astore
      case 17 | 19 | 20 | 132 => 3 // sipush, ldc_w, ldc2_w, iinc
      case opcode if 153 <= opcode && opcode <= 168 => 3 // ifeq .. jsr
      case opcode if 178 <= opcode && opcode <= 184 => 3 // getstatic .. invokestatic
      case 187 | 189 | 192 | 193 | 198 | 199 => 3 // new, anewarray, checkcast, instanceof, ifnull, ifnonnull
      case 197 => 4 // multianewarray
      case 185 | 186 | 200 | 201 => 5 // invokeinterface, invokedynamic, goto_w, jsr_w
      case _ => 1
    }
}
