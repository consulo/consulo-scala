package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.psi.scope.GlobalSearchScope
import consulo.project.content.scope.ProjectAwareSearchScope
import consulo.index.io.{InlineKeyDescriptor, KeyDescriptor}
import consulo.language.psi.stub.{AbstractStubIndex, StubIndex, StubIndexKey}
import consulo.application.util.function.CommonProcessors.alwaysFalse
import org.jetbrains.plugins.scala.finder.ScalaFilterScope
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil

import java.util
import scala.reflect.{ClassTag, classTag}

abstract class ScCharSequenceHashStubIndexExtension[Psi <: PsiElement : ClassTag] extends AbstractStubIndex[CharSequence, Psi] {

  // upstream: CharSequenceHashStubIndexExtension (absent in Consulo) - keys are stored as hashes
  override final def getKeyDescriptor: KeyDescriptor[CharSequence] = ScCharSequenceHashStubIndexExtension.CharSequenceHashInlineKeyDescriptor

  @deprecated("Deprecated base method, please use ScCharSequenceHashStubIndexExtension#getElements", "2023.3")
  @deprecatedOverriding
  override final def get(key: CharSequence, project: Project, scope: ProjectAwareSearchScope): util.Collection[Psi] = {
    getElements(key, project, scope.asInstanceOf[GlobalSearchScope])
  }

  protected def preprocessKey(key: CharSequence): CharSequence

  final def getElements(key: CharSequence, project: Project, scope: GlobalSearchScope): util.Collection[Psi] = {
    val keyPreprocessed = preprocessKey(key)
    val requiredClass = classTag[Psi].runtimeClass.asInstanceOf[Class[Psi]]
    val scalaScope = ScalaFilterScope(scope)(using project)
    StubIndex.getElements(getKey, keyPreprocessed, project, scalaScope, requiredClass)
  }

  final def hasElement(key: CharSequence, project: Project, scope: GlobalSearchScope, requiredClass: Class[Psi]): Boolean = {
    val indexKey: StubIndexKey[CharSequence, Psi] = getKey
    val keyPreprocessed: CharSequence = preprocessKey(key)

    // processElements will return true only there is no elements
    val noElements = StubIndex.getInstance.processElements(
      indexKey,
      keyPreprocessed,
      project,
      scope,
      requiredClass,
      alwaysFalse[Psi]
    )
    !noElements
  }

  override def getVersion: Int = 2 // Workaround for SCL-21752
}

object ScCharSequenceHashStubIndexExtension {

  private object CharSequenceHashInlineKeyDescriptor extends InlineKeyDescriptor[CharSequence] {
    override def fromInt(n: Int): CharSequence = new HashWrapper(n)

    override def toInt(s: CharSequence): Int = s.hashCode
  }

  private final class HashWrapper(hash: Int) extends CharSequence {
    override def hashCode: Int = hash

    override def equals(obj: Any): Boolean = obj match {
      case cs: CharSequence => cs.hashCode == hash
      case _ => false
    }

    override def length: Int = throw new UnsupportedOperationException

    override def charAt(index: Int): Char = throw new UnsupportedOperationException

    override def subSequence(start: Int, end: Int): CharSequence = throw new UnsupportedOperationException

    override def toString: String = s"hash[$hash]"
  }
}

abstract class ScFqnHashStubIndexExtension[Psi <: PsiElement : ClassTag] extends ScCharSequenceHashStubIndexExtension[Psi] {
  override protected def preprocessKey(fqn: CharSequence): CharSequence =
    ScalaNamesUtil.cleanFqn(fqn.toString)
}