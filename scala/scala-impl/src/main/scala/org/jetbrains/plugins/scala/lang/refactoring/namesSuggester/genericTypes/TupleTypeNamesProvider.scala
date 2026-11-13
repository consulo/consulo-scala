package org.jetbrains.plugins.scala.lang.refactoring.namesSuggester.genericTypes

import consulo.annotation.component.ExtensionImpl

import org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.api.TupleType

@ExtensionImpl
class TupleTypeNamesProvider extends GenericTypeNamesProvider {

  override def names(`type`: ScParameterizedType): Seq[String] =
    `type` match {
      case TupleType(_) => Seq("tuple")
      case _ => Seq.empty
    }
}
