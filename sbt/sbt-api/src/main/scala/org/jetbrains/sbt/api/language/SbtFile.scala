package org.jetbrains.sbt.api.language
import consulo.module.Module
import consulo.language.psi.stub.PsiFileWithStubSupport

trait SbtFile extends PsiFileWithStubSupport {
  def findBuildModule(module: Module): Option[Module]
}