package org.jetbrains.plugins.scala.lang.dfa.analysis.invocations.interprocedural

import com.intellij.java.analysis.impl.codeInspection.dataFlow.jvm.descriptors.JvmVariableDescriptor
import consulo.language.dataFlow.types.DfType
import consulo.language.dataFlow.value.DfaVariableValue
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeUtils.scTypeToDfType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

case class MethodResultDescriptor(method: ScFunction) extends JvmVariableDescriptor {

  override def toString: String = "METHOD_RESULT " + method.name

  override def getDfType(qualifier: DfaVariableValue): DfType = scTypeToDfType(method.returnType.getOrAny)

  override def isStable: Boolean = true
}
