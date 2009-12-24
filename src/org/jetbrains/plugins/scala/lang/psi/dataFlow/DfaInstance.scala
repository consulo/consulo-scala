package org.jetbrains.plugins.scala.lang.psi
package dataFlow

import controlFlow.Instruction

/**
 * @author ilyas
 */

trait DfaInstance[E] {
  def isForward: Boolean
  val fun: Instruction => E => E
}

trait Semilattice[E] {
  def eq(e1: E, e2: E): Boolean
  def join(ins: Iterable[E]): E
  val bottom: E
}