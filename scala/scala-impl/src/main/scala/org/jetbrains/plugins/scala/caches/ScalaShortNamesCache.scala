package org.jetbrains.plugins.scala.caches

import consulo.annotation.component.ExtensionImpl

import consulo.project.Project
import com.intellij.java.language.psi.{PsiClass, PsiField, PsiMethod}
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.language.psi.search.PsiShortNamesCache
import consulo.util.collection.ArrayUtil
import consulo.language.psi.stub.IdFilter

import java.util.function.Predicate
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTrait, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys
import org.jetbrains.plugins.scala.rt.util.ScalaBytecodeConstants.TraitImplementationClassSuffix_211

import scala.collection.mutable.ArrayBuffer

@ExtensionImpl
class ScalaShortNamesCache(implicit project: Project) extends PsiShortNamesCache {
  override def getClassesByName(name: String, scope: GlobalSearchScope): Array[PsiClass] = {
    def isOkForJava(elem: ScalaPsiElement): Boolean = {
      var res = true
      var element = elem.getParent
      while (element != null && res) {
        element match {
          case o: ScObject if o.isPackageObject => res = false
          case _ =>
        }
        element = element.getParent
      }
      res
    }

    val cacheManager = ScalaShortNamesCacheManager.getInstance(using project)

    val classes = cacheManager.getClassesByName(name, scope)
    var res = new ArrayBuffer[PsiClass]()

    classes.foreach {
      case o: ScObject if isOkForJava(o) =>
        o.fakeCompanionClass.foreach(res.+=)
      case _ =>
    }
    if (name.endsWith("$")) {
      val nameWithoutDollar = name.substring(0, name.length() - 1)
      val classes = cacheManager.getClassesByName(nameWithoutDollar, scope)

      classes.foreach {
        case c: ScTypeDefinition if isOkForJava(c) =>
          c.fakeCompanionModule.foreach(res.+=)
        case _ =>
      }
    } else if (name.endsWith(TraitImplementationClassSuffix_211)) {
      val nameWithoutDollar = name.substring(0, name.length() - 6)
      val classes = cacheManager.getClassesByName(nameWithoutDollar, scope)

      classes.foreach {
        case c: ScTrait if isOkForJava(c) =>
          res += c.fakeCompanionClass
          c.fakeCompanionModule.foreach(res.+=)
        case _ =>
      }
    } else classes.foreach {
      case c: ScTypeDefinition if isOkForJava(c) =>
        res += c
      case _ =>
    }

    res.toArray
  }

  override def processMethodsWithName(name: String, scope: GlobalSearchScope, processor: Predicate[PsiMethod]): Boolean = {
    //todo:
    true
  }

  // upstream: the following are default implementations in IntelliJ's PsiShortNamesCache
  override def processMethodsWithName(name: String, processor: Predicate[? >: PsiMethod], scope: GlobalSearchScope, filter: IdFilter): Boolean =
    processMethodsWithName(name, scope, (m: PsiMethod) => processor.test(m))

  override def processFieldsWithName(name: String, processor: Predicate[? >: PsiField], scope: GlobalSearchScope, filter: IdFilter): Boolean =
    getFieldsByName(name, scope).forall(processor.test)

  override def processClassesWithName(name: String, processor: Predicate[? >: PsiClass], scope: GlobalSearchScope, filter: IdFilter): Boolean =
    getClassesByName(name, scope).forall(processor.test)

  override def getAllClassNames(dest: java.util.HashSet[String]): Unit =
    dest.addAll(java.util.Arrays.asList(getAllClassNames*))

  override def getAllMethodNames(set: java.util.HashSet[String]): Unit =
    set.addAll(java.util.Arrays.asList(getAllMethodNames*))

  override def getAllFieldNames(set: java.util.HashSet[String]): Unit =
    set.addAll(java.util.Arrays.asList(getAllFieldNames*))

  override def getAllClassNames: Array[String] = {
    import ScalaIndexKeys._
    ALL_CLASS_NAMES.allKeys.toArray
  }

  override def getMethodsByName(name: String, scope: GlobalSearchScope): Array[PsiMethod] = {
    PsiMethod.EMPTY_ARRAY //todo:
  }

  override def getMethodsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int): Array[PsiMethod] = {
    getMethodsByName(name, scope) //todo:
  }

  override def getAllMethodNames: Array[String] = {
    ArrayUtil.EMPTY_STRING_ARRAY //todo:
  }

  override def getFieldsByName(name: String, scope: GlobalSearchScope): Array[PsiField] = {
    PsiField.EMPTY_ARRAY //todo:
  }

  override def getAllFieldNames: Array[String] = {
    ArrayUtil.EMPTY_STRING_ARRAY //todo:
  }

  override def getFieldsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int): Array[PsiField] = {
    Array.empty //todo:
  }

}
