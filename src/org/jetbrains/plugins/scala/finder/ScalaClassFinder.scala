package org.jetbrains.plugins.scala
package finder

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi._
import java.lang.String
import caches.ScalaShortNamesCacheManager
import lang.psi.impl.ScalaPsiManager
import com.intellij.openapi.project.Project
import collection.mutable.ArrayBuffer
import java.util
import lang.psi.api.toplevel.typedef.{ScTrait, ScClass, ScObject}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil

class ScalaClassFinder(project: Project) extends PsiElementFinder {
  def findClasses(qualifiedName: String, scope: GlobalSearchScope): Array[PsiClass] = {
    val res = new ArrayBuffer[PsiClass]

    def iterateClasses(suffix: String)(fun: PsiClass => Unit) {
      if (!qualifiedName.endsWith(suffix)) return
      val nameWithoutDollar = qualifiedName.substring(0, qualifiedName.length() - suffix.length)
      val classes = ScalaShortNamesCacheManager.getInstance(project).getClassesByFQName(nameWithoutDollar, scope)
      (if (classes.isEmpty) {
        val converted = ScalaPsiUtil.convertMemberName(nameWithoutDollar)
        if (nameWithoutDollar != converted) ScalaShortNamesCacheManager.getInstance(project).getClassesByFQName(converted, scope)
        else classes
      } else classes).foreach(fun)
    }

    iterateClasses("") {
      case o: ScObject if !o.isPackageObject =>
        o.fakeCompanionClass match {
          case Some(c) => res += c
          case _ =>
        }
      case _ =>
    }

    iterateClasses("$") {
      case c: ScClass =>
        c.fakeCompanionModule match {
          case Some(o) => res += o
          case _ =>
        }
      case _ =>
    }

    iterateClasses("$class") {
      case c: ScTrait => res += c.fakeCompanionClass
      case _ =>
    }

    res.toArray
  }

  def findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass = {
    val classes = findClasses(qualifiedName, scope)
    if (classes.length > 0) classes(0)
    else null
  }

  override def findPackage(qName: String): PsiJavaPackage = null

  override def getClassNames(psiPackage: PsiJavaPackage, scope: GlobalSearchScope): util.Set[String] = {
    ScalaPsiManager.instance(project).getJavaPackageClassNames(psiPackage, scope)
  }

  override def getClasses(psiPackage: PsiJavaPackage, scope: GlobalSearchScope): Array[PsiClass] = {
    val otherClassNames = getClassNames(psiPackage, scope)
    val result: ArrayBuffer[PsiClass] = new ArrayBuffer[PsiClass]()
    import scala.collection.JavaConversions._
    for (clazzName <- otherClassNames) {
      val qualName = psiPackage.getQualifiedName + "." + clazzName
      result ++= ScalaPsiManager.instance(project).getCachedClasses(scope, qualName)
      result ++= findClasses(qualName, scope)
    }
    result.toArray
  }
}