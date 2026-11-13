package org.jetbrains.plugins.scala.annotator

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.editor.intention.IntentionAction
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference

@ExtensionAPI(ComponentScope.APPLICATION)
abstract class UnresolvedReferenceFixProvider {
  def fixesFor(reference: ScReference): Seq[IntentionAction]
}

object UnresolvedReferenceFixProvider
  extends ExtensionPointDeclaration[UnresolvedReferenceFixProvider]("org.intellij.scala.unresolvedReferenceFixProvider") {

  def fixesFor(reference: ScReference): Seq[IntentionAction] =
    implementations.flatMap {
      _.fixesFor(reference)
    }
}
