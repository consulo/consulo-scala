package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.psi.scope.GlobalSearchScope
import consulo.language.psi.stub.{StringStubIndexExtension, StubIndex}
import org.jetbrains.plugins.scala.finder.ScalaFilterScope

import java.{util => ju}
import scala.reflect.{ClassTag, classTag}

abstract class ScStringStubIndexExtension[E <: PsiElement : ClassTag] extends StringStubIndexExtension[E] {

  @deprecated("Deprecated base method, please use ScStringStubIndexExtension#getElements", "2023.3")
  @deprecatedOverriding
  override final def get(key: String, project: Project, scope: GlobalSearchScope): ju.Collection[E] =
    getElements(key, project, scope)

  final def getElements(key: String, project: Project, scope: GlobalSearchScope): ju.Collection[E] = {
    val requiredClass = classTag[E].runtimeClass.asInstanceOf[Class[E]]
    val scalaScope = ScalaFilterScope(scope)(using project)
    StubIndex.getElements(getKey, key, project, scalaScope, requiredClass)
  }
}
