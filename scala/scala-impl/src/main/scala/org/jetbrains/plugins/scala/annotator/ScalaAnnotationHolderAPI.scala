package org.jetbrains.plugins.scala.annotator

import consulo.language.editor.intention.IntentionAction
import consulo.language.editor.inspection.ProblemHighlightType
import consulo.language.editor.annotation.HighlightSeverity
import consulo.colorScheme.TextAttributes
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement

/**
 * Methods similar to the deprecated API of [[com.intellij.lang.annotation.AnnotationHolder]],
 * implemented using new API
 */
trait ScalaAnnotationHolderAPI {
  this: ScalaAnnotationHolder =>

  final def createErrorAnnotation(range: TextRange,
                                  message: String,
                                  highlightType: ProblemHighlightType,
                                  fixes: Iterable[IntentionAction]): Unit = {
    val builder = newAnnotation(HighlightSeverity.ERROR, message: String)
      .range(range)
      .highlightType(highlightType)
    for (fix <- fixes) {
      builder.withFix(fix)
    }
    builder.create()
  }

  final def createErrorAnnotation(element: PsiElement,
                                  message: String,
                                  highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR,
                                  fixes: Iterable[IntentionAction] = Nil): Unit =
    createErrorAnnotation(element.getTextRange, message, highlightType, fixes)


  final def createErrorAnnotation(element: PsiElement,
                                  message: String,
                                  fix: IntentionAction): Unit =
    createErrorAnnotation(element.getTextRange, message, ProblemHighlightType.GENERIC_ERROR, Some(fix))

  final def createErrorAnnotation(range: TextRange,
                                  message: String,
                                  fixes: Iterable[IntentionAction]): Unit =
    createErrorAnnotation(range, message, ProblemHighlightType.GENERIC_ERROR, fixes)

  final def createErrorAnnotation(element: PsiElement,
                                  message: String,
                                  fixes: Iterable[IntentionAction]): Unit =
    createErrorAnnotation(element, message, ProblemHighlightType.GENERIC_ERROR, fixes)

  final def createErrorAnnotation(range: TextRange,
                                  message: String,
                                  fix: IntentionAction): Unit =
    createErrorAnnotation(range, message, ProblemHighlightType.GENERIC_ERROR, Some(fix))

  final def createErrorAnnotation(range: TextRange,
                                  message: String): Unit =
    createErrorAnnotation(range, message, ProblemHighlightType.GENERIC_ERROR, None)

  final def createWarningAnnotation(range: TextRange,
                                    message: String,
                                    highlightType: ProblemHighlightType,
                                    fixes: Iterable[IntentionAction]): Unit = {
    val builder = newAnnotation(HighlightSeverity.WARNING, message: String)
      .range(range)
      .highlightType(highlightType)

    for (f <- fixes) {
      builder.withFix(f)
    }
    builder.create()
  }

  final def createWarningAnnotation(element: PsiElement,
                                    message: String,
                                    highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    fixes: Iterable[IntentionAction] = Nil): Unit =
    createWarningAnnotation(element.getTextRange, message, highlightType, fixes)

  final def createWarningAnnotation(element: PsiElement,
                                    message: String,
                                    highlightType: ProblemHighlightType,
                                    fix: IntentionAction): Unit =
    createWarningAnnotation(element.getTextRange, message, highlightType, Some(fix))

  final def createWarningAnnotation(element: PsiElement,
                                    message: String,
                                    fix: IntentionAction): Unit =
    createWarningAnnotation(element.getTextRange, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, Some(fix))


  final def createInfoAnnotation(range: TextRange,
                                 message: String,
                                 enforcedAttributes: Option[TextAttributes],
                                 fixes: Iterable[IntentionAction]): Unit = {
    val builder = newAnnotation(HighlightSeverity.INFORMATION, message: String)
      .range(range)
    for (fix <- fixes) {
      builder.withFix(fix)
    }
    for (attr <- enforcedAttributes) {
      builder.enforcedTextAttributes(attr)
    }
    builder.create()
  }

  final def createWeakWarningAnnotation(element: PsiElement,
                                        message: String): Unit =
    newAnnotation(HighlightSeverity.WEAK_WARNING, message)
      .range(element)
      .create()
}
