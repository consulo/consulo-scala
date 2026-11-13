package org.jetbrains.plugins.scala.runner

import consulo.execution.RunManager
import consulo.language.psi.PsiElement
import consulo.language.editor.refactoring.event.{RefactoringElementListener, RefactoringElementListenerComposite, RefactoringElementListenerProvider, RefactoringListenerProvider}
import consulo.logging.Logger
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper

import scala.util.control.NonFatal

class ScalaRunConfigurationRefactoringListenerProvider extends RefactoringElementListenerProvider {
  private def wrap(td: ScTemplateDefinition) = new PsiClassWrapper(td, td.qualifiedName, td.name)
  private def decorate(listener: RefactoringElementListener): RefactoringElementListener = {
    if (listener == null) return null

    new RefactoringElementListener {
      override def elementMoved(newElement: PsiElement): Unit = newElement match {
        case td: ScTemplateDefinition => listener.elementMoved(wrap(td))
        case _ =>
      }
      override def elementRenamed(newElement: PsiElement): Unit = newElement match {
        case td: ScTemplateDefinition => listener.elementRenamed(wrap(td))
        case _ =>
      }
    }
  }

  override def getListener(element: PsiElement): RefactoringElementListener = {
    element match {
      case td: ScTemplateDefinition =>
        val wrapper = wrap(td)
        decorate(runConfigurationsListener(wrapper))
      case _ => null
    }
  }

  // upstream: super.getListener of RunConfigurationRefactoringElementListenerProvider (internal in Consulo)
  private def runConfigurationsListener(element: PsiElement): RefactoringElementListener = {
    var composite: RefactoringElementListenerComposite = null
    RunManager.getInstance(element.getProject).getAllConfigurations.foreach {
      case provider: RefactoringListenerProvider =>
        val listener =
          try provider.getRefactoringElementListener(element)
          catch {
            case NonFatal(e) =>
              ScalaRunConfigurationRefactoringListenerProvider.LOG.error(e)
              null
          }
        if (listener != null) {
          if (composite == null)
            composite = new RefactoringElementListenerComposite
          composite.addListener(listener)
        }
      case _ =>
    }
    composite
  }
}

object ScalaRunConfigurationRefactoringListenerProvider {
  private val LOG = Logger.getInstance(classOf[ScalaRunConfigurationRefactoringListenerProvider])
}
