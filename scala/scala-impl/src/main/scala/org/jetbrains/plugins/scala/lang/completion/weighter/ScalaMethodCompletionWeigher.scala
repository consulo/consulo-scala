package org.jetbrains.plugins.scala.lang.completion.weighter

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionLocation, CompletionWeigher}
import consulo.language.editor.completion.lookup.LookupElement
import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

import java.util.{Comparator, Objects}

@ExtensionImpl(id = "scalaMethodCompletionWeigher", order = "after scalaContainingClassWeigher, before proximity")
final class ScalaMethodCompletionWeigher extends CompletionWeigher {
  case class MethodNameComparable(@Nullable name: String, hasParameters: Boolean) extends Comparable[MethodNameComparable] {
    override def compareTo(o: MethodNameComparable): Int = {
      val i = Objects.compare(name, o.name, Comparator.nullsLast[String](_.compareTo(_)))
      if (i != 0) return 0
      if (hasParameters == o.hasParameters) 0
      else if (hasParameters && !o.hasParameters) 1
      else -1
    }
  }

  override def weigh(element: LookupElement, location: CompletionLocation): Comparable[?] = element match {
    case ScalaLookupItem(_, namedElement) =>
      namedElement match {
        case psi: ScFunction =>
          MethodNameComparable(psi.name, psi.parameters.nonEmpty)
        case psi: PsiMethod =>
          MethodNameComparable(psi.name, psi.getParameterList.getParametersCount > 0)
        case _ => null
      }
    case _ => null //do not compare to anything in Java
  }
}
