package org.jetbrains.plugins.scala.lang.psi.impl

import consulo.util.dataholder.Key
import consulo.language.psi.PsiElement
import consulo.annotation.component.{ComponentScope, TopicAPI}

object CompilerType {
  private val TypeKey: Key[String] = Key.create("SCALA_COMPILER_TYPE_KEY")

  private val TypeRequestedKey: Key[AnyRef] = Key.create("SCALA_COMPILER_TYPE_REQUESTED_KEY")

  val Topic: Class[CompilerTypeListener] = classOf[CompilerTypeListener]

  type Listener = CompilerTypeListener

  def apply(e: PsiElement): Option[String] =
    Option(e.getCopyableUserData(TypeKey))

  def update(e: PsiElement, tpe: Option[String]): Unit = {
    e.putCopyableUserData(TypeKey, tpe.orNull)
  }

  def requestFor(e: PsiElement): Unit = {
    // Request only once per element
    if (e.getUserData(TypeRequestedKey) == null) {
      e.putUserData(TypeRequestedKey, "")
      e.getProject.getMessageBus.syncPublisher(Topic).onCompilerTypeRequest(e)
    }
  }
}

// upstream: CompilerType.Listener (Consulo topics must be top level)
@TopicAPI(ComponentScope.PROJECT)
trait CompilerTypeListener {
  def onCompilerTypeRequest(e: PsiElement): Unit
}
