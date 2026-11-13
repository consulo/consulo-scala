package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import java.util.function.Consumer
import consulo.codeEditor.Editor
import consulo.language.psi.PsiElement
import consulo.language.editor.refactoring.event.RefactoringElementListener
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor
import consulo.usage.UsageInfo
import org.jetbrains.plugins.scala.lang.psi.fake.FakePsiMethod
import org.jetbrains.plugins.scala.lang.psi.light.{PsiTypedDefinitionWrapper, ScFunctionWrapper, StaticPsiMethodWrapper, StaticPsiTypedDefinitionWrapper}

import java.util

@ExtensionImpl(order = "first, before KotlinAwareJavaGetter")
class RenameLightProcessor extends RenamePsiElementProcessor {
  override def canProcessElement(element: PsiElement): Boolean = {
    element match {
      case _: FakePsiMethod => true
      case _: ScFunctionWrapper => true
      case _: PsiTypedDefinitionWrapper => true
      case _: StaticPsiTypedDefinitionWrapper => true
      case _: StaticPsiMethodWrapper => true
      case _ => false
    }
  }


  override def prepareRenaming(element: PsiElement, newName: String, allRenames: util.Map[PsiElement, String]): Unit = {
    val orig = originalElement(element)
    allRenames.put(orig, newName)
    import scala.jdk.CollectionConverters._
    for (processor <- RenamePsiElementProcessor.allForElement(orig).asScala) {
      processor.prepareRenaming(orig, newName, allRenames)
    }
  }

  override def substituteElementToRename(element: PsiElement, editor: Editor): PsiElement = {
    val orig = originalElement(element)
    if (orig != null) {
      val processor = RenamePsiElementProcessor.forElement(orig)
      processor.substituteElementToRename(orig, editor)
    } else null
  }

  override def substituteElementToRename(element: PsiElement, editor: Editor, renameCallback: Consumer[PsiElement]): Unit = {
    val orig = originalElement(element)
    if (orig != null) {
      val processor = RenamePsiElementProcessor.forElement(orig)
      processor.substituteElementToRename(orig, editor, renameCallback)
    }
  }

  private def originalElement(element: PsiElement) = element match {
    case _: FakePsiMethod => null
    case ScFunctionWrapper(delegate) => delegate
    case PsiTypedDefinitionWrapper(delegate) => delegate
    case StaticPsiTypedDefinitionWrapper(delegate) => delegate
    case StaticPsiMethodWrapper(method) => method
    case _ => element
  }

  override def renameElement(element: PsiElement, newName: String, usages: Array[UsageInfo], listener: RefactoringElementListener): Unit = {
    ScalaRenameUtil.doRenameGenericNamedElement(element, newName, usages, listener)
  }
}
