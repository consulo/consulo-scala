package org.jetbrains.plugins.scala.lang.completion.clauses

import consulo.language.editor.completion.{CompletionParameters, CompletionProvider, CompletionResultSet}
import consulo.language.psi.util.PsiTreeUtil.getContextOfType
import consulo.language.util.ProcessingContext
import org.jetbrains.plugins.scala.lang.completion.positionFromParameters
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

import scala.reflect.{ClassTag, classTag}

private[clauses] abstract class ClauseCompletionProvider[
  T <: ScalaPsiElement & Typeable : ClassTag
] extends CompletionProvider[CompletionParameters] {

  override final def addCompletions(parameters: CompletionParameters,
                                    context: ProcessingContext,
                                    result: CompletionResultSet): Unit = {
    val place = positionFromParameters(using parameters)
    val typeable = getContextOfType(place, classTag.runtimeClass.asInstanceOf[Class[T]])
    if (typeable != null) {
      val originalFile = parameters.getOriginalFile
      val clauseParameters = ClauseCompletionParameters(place, originalFile.getResolveScope, parameters.getInvocationCount)
      addCompletions(typeable, result)(using clauseParameters)
    }
  }

  protected def addCompletions(typeable: T, result: CompletionResultSet)
                              (implicit parameters: ClauseCompletionParameters): Unit
}
