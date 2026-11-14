package org.jetbrains.plugins.scala.debugger

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.SourcePosition
import com.intellij.java.debugger.impl.engine.SourcePositionProvider
import com.intellij.java.debugger.impl.{DebuggerContextImpl, DebuggerContextUtil, PositionUtil}
import com.intellij.java.debugger.impl.ui.tree.{FieldDescriptor, LocalVariableDescriptor}
import com.intellij.java.debugger.ui.tree.NodeDescriptor
import consulo.application.dumb.IndexNotReadyException
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator.isIdentifier

import scala.annotation.tailrec

@ExtensionImpl
class ScalaSourcePositionProvider extends SourcePositionProvider {
  override def computeSourcePosition(descriptor: NodeDescriptor,
                                     project: Project,
                                     context: DebuggerContextImpl,
                                     nearest: Boolean): SourcePosition = {

    val contextElement = PositionUtil.getContextElement(context)
    if (contextElement == null) return null

    val isScala = contextElement.getLanguage.isKindOf(ScalaLanguage.INSTANCE)
    if (!isScala) return null

    descriptor match {
      case _: FieldDescriptor | _: LocalVariableDescriptor =>
      case _ => return null
    }

    def calculateSourcePosition(element: PsiElement): Option[SourcePosition] = {
      val containingFile = element.getContainingFile
      if (containingFile eq null) return None

      val position =
        try {
          if (nearest) DebuggerContextUtil.findNearest(context, element, containingFile)
          else SourcePosition.createFromElement(element)
        } catch {
          case _: IndexNotReadyException => SourcePosition.createFromElement(element)
        }

      Option(position)
    }

    val name = descriptor.getName
    resolveReferenceWithName(name, contextElement) match {
      case bp: ScBindingPattern => calculateSourcePosition(bp).orNull
      case cp: ScClassParameter => calculateSourcePosition(cp).orNull
      case _ => null
    }
  }

  @tailrec
  private def resolveReferenceWithName(name: String, context: PsiElement): PsiElement = {
    if (!isIdentifier(name)) return null
    if (name == "$outer" || name.startsWith("x$")) return null

    val ref = ScalaPsiElementFactory.createExpressionWithContextFromText(name, context, context).asInstanceOf[ScReferenceExpression]

    ref.resolve() match {
      case null if name.contains("$") =>
        val fixedName = name.substring(0, name.indexOf('$'))
        resolveReferenceWithName(fixedName, context)
      case elem => elem
    }
  }
}
