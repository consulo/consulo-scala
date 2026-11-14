package org.jetbrains.plugins.scala.testingSupport

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.codeInsight.TestFrameworks
import consulo.util.lang.Pair
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.language.psi.search.PsiShortNamesCache
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.{PsiElement, PsiNamedElement}
import com.intellij.java.impl.testIntegration.JavaTestFinder
import consulo.language.editor.testIntegration.TestFinderHelper
import consulo.language.util.ModuleUtilCore
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

import java.util.Collections
import java.util.regex.Pattern

@ExtensionImpl
class ScalaTestFinder extends JavaTestFinder {
  override def findTestsForClass(element: PsiElement): java.util.Collection[PsiElement] = {
    findSourceElement(element) match {
      case obj: ScObject =>
        val objName = obj.getName.stripSuffix("$")
        val pattern = Pattern.compile(s".*$objName.*", Pattern.CASE_INSENSITIVE)
        val frameworks: TestFrameworks = TestFrameworks.getInstance
        val cache = PsiShortNamesCache.getInstance(obj.getProject)
        val scope: GlobalSearchScope = getSearchScope(obj, false)
        val names = cache.getAllClassNames.distinct
        val res = new java.util.ArrayList[Pair[? <: PsiNamedElement, Integer]]()
        names.foreach { testClassName =>
          if (pattern.matcher(testClassName).matches()) {
            for (testClass <- cache.getClassesByName(testClassName, scope)) {
              if (frameworks.isTestClass(testClass) || frameworks.isPotentialTestClass(testClass)) {
                res.add(Pair.create(testClass, TestFinderHelper.calcTestNameProximity(objName, testClassName)))
              }
            }
          }
        }
        TestFinderHelper.getSortedElements(res, true)
      case _ => //do nothing, java finder will find the classes
        Collections.emptyList()
    }
  }

  override def findClassesForTest(element: PsiElement): java.util.Collection[PsiElement] = {
    val klass: PsiClass = findSourceElement(element)
    if (klass == null) return Collections.emptySet[PsiElement]
    val scope: GlobalSearchScope = getSearchScope(element, true)
    val cache: PsiShortNamesCache = PsiShortNamesCache.getInstance(element.getProject)
    val classesWithWeights: java.util.List[Pair[? <: PsiNamedElement, Integer]] =
      new java.util.ArrayList[Pair[? <: PsiNamedElement, Integer]]()

    TestFinderHelper.collectPossibleClassNamesWithWeights(klass.getName).forEach { nameWithWeight =>
      for (aClass <- cache.getClassesByName(nameWithWeight.first, scope)) {
        if (isTestSubjectClass(aClass)) classesWithWeights.add(Pair.create(aClass, nameWithWeight.second))
      }
      for (anObject <- cache.getClassesByName(nameWithWeight.first + "$", scope)) {
        if (isTestSubjectClass(anObject)) classesWithWeights.add(Pair.create(anObject, nameWithWeight.second))
      }
    }
    TestFinderHelper.getSortedElements(classesWithWeights, false)
  }

  // upstream: protected methods of JavaTestFinder, private in Consulo
  private def getSearchScope(element: PsiElement, dependencies: Boolean): GlobalSearchScope =
    ModuleUtilCore.findModuleForPsiElement(element) match {
      case null   => GlobalSearchScope.projectScope(element.getProject)
      case module =>
        if (dependencies) GlobalSearchScope.moduleWithDependenciesScope(module)
        else GlobalSearchScope.moduleWithDependentsScope(module)
    }

  private def isTestSubjectClass(klass: PsiClass): Boolean =
    !(klass.isAnnotationType || TestFrameworks.getInstance.isTestClass(klass) || !klass.isPhysical)
}
