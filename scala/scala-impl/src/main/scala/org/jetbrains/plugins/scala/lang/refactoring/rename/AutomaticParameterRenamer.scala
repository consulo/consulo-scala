package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.java.impl.refactoring.JavaRefactoringBundle
import com.intellij.java.language.JavaLanguage
import consulo.util.lang.Comparing
import com.intellij.java.indexing.search.searches.OverridingMethodsSearch
import consulo.language.psi.PsiElement
import com.intellij.java.language.psi.{PsiMethod, PsiParameter}
import consulo.language.editor.refactoring.RefactoringBundle
import consulo.language.editor.refactoring.rename.{AutomaticRenamer, AutomaticRenamerFactory}
import consulo.usage.UsageInfo
import org.jetbrains.plugins.scala.extensions.PsiNamedElementExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExtension, ScFunction, ScFunctionExt}
import org.jetbrains.plugins.scala.lang.psi.light.ScFunctionWrapper
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import java.util

final class AutomaticParameterRenamer(param: PsiParameter, newName: String) extends AutomaticRenamer {
  locally {
    val owner = param match {
      case p: ScParameter => p.owner
      case _ => param.getDeclarationScope
    }

    owner match {
      case method: PsiMethod =>
        suggestForMethod(method)
      case ext: ScExtension =>
        ext.extensionMethods.foreach(suggestForMethod)
      case _ =>
    }
  }

  override def getDialogTitle: String = JavaRefactoringBundle.message("rename.parameters.dialog.title")

  override def getDialogDescription: String = RefactoringBundle.message("title.rename.parameters.hierarchy")

  override def entityName(): String = JavaRefactoringBundle.message("automatic.parameter.renamer.entity.name")

  override def isSelectedByDefault: Boolean = true

  private def suggestForMethod(method: PsiMethod): Unit = {
    val (idx, invokedOnExtensionMethod) = method match {
      case fun: ScFunction if fun.isExtensionMethod =>
        fun.parameterClausesWithExtension().flatMap(_.parameters).indexOf(param) -> true
      case _ =>
        method.getParameterList.getParameterIndex(param) -> false
    }

    if (idx >= 0) {
      val overriders = OverridingMethodsSearch.search(method).findAll()
      overriders.forEach {
        case wrapper: ScFunctionWrapper =>
          val parameters = wrapper.delegate.parameterClausesWithExtension().flatMap(_.parameters)
          suggestForParam(parameters, idx)
        case fun: ScFunction =>
          val parameters = fun.parameterClausesWithExtension().flatMap(_.parameters)
          suggestForParam(parameters, idx)
        case method: PsiMethod if invokedOnExtensionMethod && method.getLanguage.isKindOf(JavaLanguage.INSTANCE) =>
          // Java cannot handle Scala 3 extensions correctly, so we have to help here
          val parameters = method.getParameterList.getParameters.toSeq
          suggestForParam(parameters, idx)
        case _ =>
      }
    }
  }

  private def suggestForParam(parameters: Seq[PsiParameter], parameterIndex: Int): Unit = {
    if (parameters.lengthCompare(parameterIndex) >= 0) {
      val inheritedParam = parameters(parameterIndex)
      if (!Comparing.strEqual(inheritedParam.name, newName)) {
        myElements.add(inheritedParam)
        suggestAllNames(inheritedParam.name, newName)
      }
    }
  }
}

@ExtensionImpl(order = "before AutomaticParametersRenamerFactory, before AutomaticVariableRenamerFactory, before AutomaticInheritorRenamerFactory, before AutomaticOverloadsRenamerFactory, before AutomaticTestRenamerFactory, before ConstructorParameterOnFieldRenameRenamerFactory")
final class AutomaticParameterRenamerFactory extends AutomaticRenamerFactory {
  override def isApplicable(element: PsiElement): Boolean = element match {
    case param: PsiParameter => param.getNameIdentifier != null
    case _ => false
  }

  override def createRenamer(element: PsiElement, newName: String, usages: util.Collection[UsageInfo]): AutomaticRenamer =
    new AutomaticParameterRenamer(element.asInstanceOf[PsiParameter], newName)

  override def isEnabled: Boolean = ScalaApplicationSettings.getInstance().RENAME_PARAMETER_IN_HIERARCHY

  override def setEnabled(enabled: Boolean): Unit =
    ScalaApplicationSettings.getInstance().RENAME_PARAMETER_IN_HIERARCHY = enabled

  override def getOptionName: LocalizeValue = LocalizeValue.of(RefactoringBundle.message("rename.parameters.hierarchy"))
}
