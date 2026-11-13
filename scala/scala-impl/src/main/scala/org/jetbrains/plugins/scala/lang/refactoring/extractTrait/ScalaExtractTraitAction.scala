package org.jetbrains.plugins.scala.lang.refactoring.extractTrait

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef

import consulo.language.editor.refactoring.RefactoringSupportProvider
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.refactoring.ScalaRefactoringSupportProvider

@ActionImpl(id = "Scala.ExtractTrait", parents = Array(new ActionParentRef(value = new ActionRef(id = "IntroduceActionsGroup"))))
class ScalaExtractTraitAction extends BasePlatformRefactoringAction(
  LocalizeValue.of(ScalaBundle.message("extract.trait.action.text")),
  LocalizeValue.of(ScalaBundle.message("extract.trait.action.description"))
) {

  override def isAvailableInEditorOnly: Boolean = false

  override def getRefactoringHandler(provider: RefactoringSupportProvider): ScalaExtractTraitHandler = provider match {
    case _: ScalaRefactoringSupportProvider => new ScalaExtractTraitHandler
    case _ => null
  }
}
