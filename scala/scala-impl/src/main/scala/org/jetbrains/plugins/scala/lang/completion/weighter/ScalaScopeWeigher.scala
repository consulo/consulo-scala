package org.jetbrains.plugins.scala.lang.completion.weighter

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionLocation, CompletionWeigher}
import consulo.language.editor.completion.lookup.LookupElement
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem
import org.jetbrains.plugins.scala.lang.completion.positionFromParameters

@ExtensionImpl(id = "ScopeWeigher", order = "after scalaKindWeigher, before proximity")
final class ScalaScopeWeigher extends CompletionWeigher {

  override def weigh(element: LookupElement, location: CompletionLocation): Comparable[?] = element match {
    case ScalaLookupItem(_, namedElement) =>
      val scopes = namedElement.scopes
      if (scopes.hasNext) checkByContext(positionFromParameters(using location.getCompletionParameters), scopes.next())
      else null
    case _ => null
  }

  private def checkByContext(first: PsiElement, second: PsiElement): Integer = {
    if (PsiTreeUtil.isContextAncestor(second, first, true))
      first.contexts.indexOf(second) match {
        case -1 => null
        case index => -index
      }
    else null
  }
}
