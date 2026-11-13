package org.jetbrains.plugins.scala.codeInsight.template.util

import consulo.language.psi.PsiNamedElement
import consulo.language.psi.resolve.ResolveState
import org.jetbrains.plugins.scala.lang.resolve.processor.BaseProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ResolveTargets, ScalaResolveResult}
import org.jetbrains.plugins.scala.project.ProjectContext

class VariablesCompletionProcessor(override val kinds: Set[ResolveTargets.Value])
                                  (implicit ctx: ProjectContext) extends BaseProcessor(kinds) {

  override protected def execute(namedElement: PsiNamedElement)
                                (implicit state: ResolveState): Boolean = {
    candidatesSet = candidatesSet + new ScalaResolveResult(namedElement)
    true
  }
}