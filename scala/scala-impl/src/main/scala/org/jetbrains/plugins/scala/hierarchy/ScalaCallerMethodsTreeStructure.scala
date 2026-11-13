package org.jetbrains.plugins.scala.hierarchy

import com.intellij.java.impl.ide.hierarchy.call.CallHierarchyNodeDescriptor
import consulo.language.editor.hierarchy.{HierarchyNodeDescriptor, HierarchyScope, HierarchyTreeStructure}
import consulo.project.Project
import com.intellij.java.language.psi.{PsiClass, PsiMember, PsiMethod}
import consulo.language.psi.{PsiElement, PsiReference}
import consulo.content.scope.SearchScope
import com.intellij.java.indexing.search.searches.MethodReferencesSearch
import consulo.language.psi.util.PsiTreeUtil
import consulo.util.collection.ArrayUtil
import consulo.application.util.function.Processor
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember

import scala.collection.mutable

final class ScalaCallerMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: HierarchyScope)
  extends HierarchyTreeStructure(project, new CallHierarchyNodeDescriptor(project, null, method, true, false)) {

  override protected def buildChildren(descriptor: HierarchyNodeDescriptor): Array[AnyRef] = {
    val enclosingElement: PsiMember = descriptor.asInstanceOf[CallHierarchyNodeDescriptor].getEnclosingElement
    if (!enclosingElement.isInstanceOf[PsiMethod]) {
      return ArrayUtil.EMPTY_OBJECT_ARRAY
    }
    val method: PsiMethod = enclosingElement.asInstanceOf[PsiMethod]
    val baseMethod: PsiMethod = getBaseDescriptor.asInstanceOf[CallHierarchyNodeDescriptor].getTargetElement.asInstanceOf[PsiMethod]
    val containing = baseMethod match {
      case mem: ScMember => mem.getContainingClassLoose
      case x => x.containingClass
    }
    val searchScope: SearchScope = getSearchScope(scopeType, containing)
    val originalClass: PsiClass = method.containingClass
    assert(originalClass != null)
    val methodsToFind = new mutable.HashSet[PsiMethod]
    methodsToFind += method
    methodsToFind ++= {
      method match {
        case fun: ScFunction => fun.superMethods
        case _ => method.findDeepestSuperMethods
      }
    }
    val methodToDescriptorMap = new mutable.HashMap[PsiMember, CallHierarchyNodeDescriptor]
    for (methodToFind <- methodsToFind) {
      MethodReferencesSearch.search(methodToFind, searchScope, true).forEach(new Processor[PsiReference] {
        override def process(reference: PsiReference): Boolean = {
          val element: PsiElement = reference.getElement
          val key: PsiMember = PsiTreeUtil.getNonStrictParentOfType(element, classOf[PsiMethod], classOf[PsiClass])
          methodToDescriptorMap synchronized {
            val d: CallHierarchyNodeDescriptor = methodToDescriptorMap.get(key) match {
              case Some(call) =>
                if (!call.hasReference(reference)) {
                  call.incrementUsageCount()
                }
                call
              case _ =>
                val newD = new CallHierarchyNodeDescriptor(myProject, descriptor, element, false, true)
                methodToDescriptorMap.put(key, newD)
                newD
            }
            d.addReference(reference)
          }
          true
        }
      })
    }
    methodToDescriptorMap.values.toArray
  }

  override def isAlwaysShowPlus: Boolean = {
    true
  }

}

