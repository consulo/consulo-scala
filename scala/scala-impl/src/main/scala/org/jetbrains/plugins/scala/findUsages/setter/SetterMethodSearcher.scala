package org.jetbrains.plugins.scala.findUsages.setter

import consulo.annotation.component.ExtensionImpl

import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.search.{PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import consulo.content.scope.SearchScope
import consulo.application.util.function.Processor
import consulo.application.util.query.QueryExecutor
import org.jetbrains.plugins.scala.extensions.{&, Parent, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.fake.FakePsiMethod
import org.jetbrains.plugins.scala.lang.psi.light.PsiTypedDefinitionWrapper
import java.util.function.Predicate


@ExtensionImpl
class SetterMethodSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {
  private val suffixScala = "_="
  private val suffixJava = "_$eq"

  override def execute(queryParameters: ReferencesSearch.SearchParameters, cons: Predicate[? >: PsiReference]): Boolean = {
    implicit val scope: SearchScope = inReadAction(queryParameters.getEffectiveSearchScope)
    implicit val consumer: Predicate[? >: PsiReference] = cons
    val element = queryParameters.getElementToSearch
    val project = queryParameters.getProject
    val data: Option[(ScNamedElement, String)] = inReadAction {
      element match {
        case _ if !element.isValid => None
        case f: ScFunction => Some((f, f.name))
        case (rp: ScReferencePattern) & inNameContext(_: ScVariable) => Some((rp, rp.name))
        case _ => None
      }
    }
    data match {
      case Some((fun: ScFunction, name)) if name `endsWith` suffixScala =>
        processAssignments(fun, name, project)
        processSimpleUsages(fun, name, project)
      case Some((pattern: ScReferencePattern, name)) =>
        processAssignments(pattern, name, project)
        processSimpleUsages(pattern, name + suffixScala, project)
        processSimpleUsages(pattern, name + suffixJava, project)
      case _ => true
    }
  }

  private def processAssignments(element: PsiElement, name: String, project: Project)
                                (implicit consumer: Predicate[? >: PsiReference], scope: SearchScope): Boolean = {
    val processor = new TextOccurenceProcessor {
      override def execute(elem: PsiElement, offsetInElement: Int): Boolean = {
        inReadAction {
          elem match {
            case Parent(Parent(assign: ScAssignment)) => assign.resolveAssignment match {
              case Some(res) if res.element.getNavigationElement == element =>
                Option(assign.leftExpression).foreach {
                  case ref: ScReference => if (!consumer.test(ref)) return false
                }
              case _ =>
            }
            case _ =>
          }
          true
        }
      }
    }
    val helper: PsiSearchHelper = PsiSearchHelper.getInstance(project)
    helper.processElementsWithWord(processor, scope, name.stripSuffix(suffixScala), UsageSearchContext.IN_CODE, true)
  }

  private def processSimpleUsages(element: PsiElement, name: String, project: Project)
                                 (implicit consumer: Predicate[? >: PsiReference], scope: SearchScope): Boolean = {
    val processor = new TextOccurenceProcessor {
      override def execute(elem: PsiElement, offsetInElement: Int): Boolean = {
        inReadAction {
          elem match {
            case ref: PsiReference => ref.resolve() match {
              case FakePsiMethod(`element`) => if (!consumer.test(ref)) return false
              case PsiTypedDefinitionWrapper(`element`) => if (!consumer.test(ref)) return false
              case _ =>
            }
            case _ =>
          }
        }
        true
      }
    }
    val helper: PsiSearchHelper = PsiSearchHelper.getInstance(project)
    helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
  }
}
