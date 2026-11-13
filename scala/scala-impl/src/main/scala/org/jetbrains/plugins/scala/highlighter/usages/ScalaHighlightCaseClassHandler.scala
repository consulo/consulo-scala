package org.jetbrains.plugins.scala.highlighter.usages

import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase
import consulo.codeEditor.Editor
import consulo.language.psi.scope.LocalSearchScope
import consulo.content.scope.SearchScope
import consulo.language.psi.{PsiElement, PsiFile}
import java.util.function.Consumer
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.findUsages.factory.{ScalaFindUsagesConfiguration, ScalaFindUsagesHandler}
import org.jetbrains.plugins.scala.incremental
import org.jetbrains.plugins.scala.incremental.Highlighting.ElementHighlightingExt
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScReference, ScStableCodeReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScSelfInvocation
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

import java.util

class ScalaHighlightCaseClassHandler(reference: ScReference, caseClass: ScClass, file: PsiFile, editor: Editor)
  extends HighlightUsagesHandlerBase[PsiElement](editor, file)
{
  override def getTargets: util.List[PsiElement] = util.Collections.singletonList(reference)

  override def selectTargets(targets: util.List[PsiElement], selectionConsumer: Consumer[util.List[PsiElement]]): Unit =
    selectionConsumer.accept(targets)

  override def addOccurrence(element: PsiElement): Unit = {
    if (element != null && element.getContainingFile == file)
      super.addOccurrence(element match {
        case ref: ScStableCodeReference => ref.nameId
        case e => e
      })
  }

  override def computeUsages(targets: util.List[PsiElement]): Unit = {
    val config = ScalaFindUsagesConfiguration.getInstance(file.getProject)
    val manager = new ScalaFindUsagesHandler(caseClass, config)
    val localSearchScope =
      if (incremental.Highlighting.enabledIn(editor.getProject))
        file.elements(_.isVisible(editor.getProject, file)).map(new LocalSearchScope(_)).foldLeft[SearchScope](LocalSearchScope.EMPTY)(_.union(_))
      else
        new LocalSearchScope(file)

    addOccurrence(caseClass.nameId)

    //highlight references to the constructor or class
    val references = manager.findReferencesToHighlight(caseClass, localSearchScope)
    references.forEach { ref =>
      val occurenceElement = ref match {
        case si: ScSelfInvocation => si.thisElement
        case _ => ref.getElement
      }
      addOccurrence(occurenceElement)
    }
  }
}
